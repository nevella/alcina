package cc.alcina.framework.servlet.publication;

import java.io.InputStream;
import java.util.Date;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.servlet.publication.ContentRenderer.ContentRendererResults;
import cc.alcina.framework.servlet.publication.FormatConverter.FormatConversionModel;
import cc.alcina.framework.servlet.publication.delivery.ContentDelivery;

/**
 * <p>
 * Process a content definition and deliver it to a delivery model (e.g. email a
 * list of documents, convert a document to pdf and serve etc)
 * </p>
 * <h3>Pipeline</h3>
 * <ol>
 * <li>ContentModelHandler: convert a content definition (say a search
 * description) to a list of objects (say search results)
 * <li>ContentRenderer: convert the list of objects to an output format (most
 * often xhtml)
 * <li>ContentWrapper: wrap content (add header, footer, that sort of
 * decoration)
 * <li>FormatConverter: at this stage output is either html or some binary
 * format - if html, can convert to Word, PDF etc
 * <li>ContentDelivery: deliver (email, download etc)
 * </ol>
 * 
 * 
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class Publisher {
	private PublicationContext ctx;

	public PublicationResult publish(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel) throws Exception {
		return publish(contentDefinition, deliveryModel, null);
	}

	@SuppressWarnings("unchecked")
	public PublicationResult publish(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Publication original) throws Exception {
		int depth = LooseContext.depth();
		try {
			ctx = new PublicationContext();
			ctx.logger = Logger.getLogger(getClass());
			ctx.contentDefinition = contentDefinition;
			ctx.deliveryModel = deliveryModel;
			LooseContext.pushWithKey(
					PublicationContext.CONTEXT_PUBLICATION_CONTEXT, ctx);
			return publish0(contentDefinition, deliveryModel, original);
		} catch (Exception e) {
			ctx.logPublicationException(e);
			throw e;
		} finally {
			LooseContext.pop();
			LooseContext.confirmDepth(depth);
		}
	}

	@SuppressWarnings("unchecked")
	private PublicationResult publish0(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Publication original) throws Exception {
		ContentModelHandler cmh = (ContentModelHandler) Registry.get()
				.instantiateSingle(ContentModelHandler.class,
						contentDefinition.getClass());
		cmh.prepareContent(contentDefinition, deliveryModel);
		if (!cmh.hasResults) {
			return null;// throw exception??
		}
		PublicationResult result = new PublicationResult();
		ctx.publicationResult = result;
		long publicationUserId = 0;
		long publicationId = 0;
		boolean forPublication = !deliveryModel.isNoPersistence()
				&& deliveryModel.provideContentDeliveryType().isRepublishable();
		PublicationContentPersister publicationContentPersister = Registry
				.implOrNull(PublicationContentPersister.class);
		if (forPublication && publicationContentPersister != null
				&& !AppPersistenceBase.isInstanceReadOnly()) {
			publicationUserId = Registry.impl(PublicationPersistence.class)
					.getNextPublicationIdForUser(
							PermissionsManager.get().getUser());
			publicationId = persist(contentDefinition, deliveryModel,
					publicationUserId, original, publicationContentPersister);
			result.publicationId = publicationId;
		}
		PublicationContent publicationContent = cmh.getPublicationContent();
		ctx.publicationContent = publicationContent;
		ContentRenderer crh = (ContentRenderer) Registry.get()
				.instantiateSingle(ContentRenderer.class,
						publicationContent.getClass());
		crh.renderContent(contentDefinition, publicationContent, deliveryModel,
				publicationId, publicationUserId);
		if (crh.getResults().persist && publicationContentPersister != null
				&& publicationId != 0) {
			publicationContentPersister.persistContentRendererResults(
					crh.getResults(), publicationId);
		}
		ContentWrapper cw = (ContentWrapper) Registry.get().instantiateSingle(
				ContentWrapper.class, publicationContent.getClass());
		cw.wrapContent(contentDefinition, publicationContent, deliveryModel,
				crh.getResults(), publicationId, publicationUserId);
		if (deliveryModel.provideContentDeliveryType().getClass() == null) {
			return null;
		}
		result.content = cw.wrappedContent;
		if (deliveryModel.provideContentDeliveryType() == ContentDeliveryType.PRINT) {
			if (result.content == null & AppPersistenceBase.isTest()) {
				result.content = Base64Utils.toBase64(cw.wrappedBytes);
			}
			return result;
		}
		FormatConverter fc = (FormatConverter) Registry.get()
				.instantiateSingle(FormatConverter.class,
						deliveryModel.provideTargetFormat().getClass());
		FormatConversionModel fcm = new FormatConversionModel();
		fcm.html = cw.wrappedContent;
		fcm.footer = cw.wrappedFooter;
		fcm.bytes = cw.wrappedBytes;
		fcm.custom = cw.custom;
		InputStream convertedContent = fc.convert(ctx, fcm);
		ContentDelivery deliverer = (ContentDelivery) Registry.get()
				.instantiateSingle(ContentDeliveryType.class,
						deliveryModel.provideContentDeliveryType().getClass());
		String token = deliverer.deliver(ctx, convertedContent, deliveryModel,
				fc);
		result.content = null;
		result.contentToken = token;
		ctx.getVisitorOrNoop().publicationFinished(result);
		return result;
	}

	private long persist(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Long publicationUserId,
			Publication original,
			PublicationContentPersister publicationContentPersister) {
		Publication publication = publicationContentPersister
				.newPublicationInstance();
		if (contentDefinition instanceof HasId) {
			HasId hasId = (HasId) contentDefinition;
			hasId.setId(0);
			// force new
		}
		if (deliveryModel instanceof HasId) {
			HasId hasId = (HasId) deliveryModel;
			hasId.setId(0);
			// force new
		}
		publication.setContentDefinition(contentDefinition);
		publication.setDeliveryModel(deliveryModel);
		publication.setUser(PermissionsManager.get().getUser());
		publication.setPublicationDate(new Date());
		publication.setOriginalPublication(original);
		publication.setUserPublicationId(publicationUserId);
		publication.setPublicationType(contentDefinition.getPublicationType());
		return Registry.impl(CommonPersistenceProvider.class)
				.getCommonPersistence().merge(publication);
	}

	public interface PublicationContentPersister {
		public Publication newPublicationInstance();

		public void persistContentRendererResults(
				ContentRendererResults results, long publicationId);

		public ContentRendererResults getContentRendererResults(
				long publicationId);
	}
}
