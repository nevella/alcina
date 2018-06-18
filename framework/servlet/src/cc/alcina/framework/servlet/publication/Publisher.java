package cc.alcina.framework.servlet.publication;

import java.io.InputStream;
import java.util.Date;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.util.CommonUtils;
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

	public PublicationContext getContext() {
		return this.ctx;
	}

	public PublicationResult publish(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel) throws Exception {
		return publish(contentDefinition, deliveryModel, null);
	}

	@SuppressWarnings("unchecked")
	public PublicationResult publish(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Publication original)
			throws Exception {
		int depth = LooseContext.depth();
		try {
			ctx = PublicationContext.setupContext(contentDefinition,
					deliveryModel);
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

	private void persist(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Long publicationUserId,
			Publication original,
			PublicationContentPersister publicationContentPersister,
			PublicationResult result) {
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
		publication.setPublicationUid(SEUtilities.generateId());
		publication.setPublicationType(contentDefinition.getPublicationType());
		try {
			PermissionsManager.get().pushCurrentUser();
			long id = Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence().merge(publication);
			result.publicationId = id;
			result.publicationUid = publication.getPublicationUid();
		} finally {
			PermissionsManager.get().popUser();
		}
	}

	private void postDeliveryPersistence(Long publicationId) {
		if (getContext().mimeMessageId != null) {
			Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence().updatePublicationMimeMessageId(
							publicationId, getContext().mimeMessageId);
		}
	}

	@SuppressWarnings("unchecked")
	private PublicationResult publish0(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Publication original)
			throws Exception {
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
		boolean forPublication = !deliveryModel.isNoPersistence()
				&& deliveryModel.provideContentDeliveryType().isRepublishable();
		PublicationContentPersister publicationContentPersister = Registry
				.implOrNull(PublicationContentPersister.class);
		PublicationContent publicationContent = cmh.getPublicationContent();
		ctx.publicationContent = publicationContent;
		result.publicationUid = deliveryModel.getPublicationUid();
		if (forPublication && publicationContentPersister != null
				&& !AppPersistenceBase.isInstanceReadOnly()) {
			IUser user = PermissionsManager.get().getUser();
			if (user == null) {
				user = CommonPersistenceProvider.get().getCommonPersistence()
						.getSystemUser(true);
			}
			publicationUserId = Registry.impl(PublicationPersistence.class)
					.getNextPublicationIdForUser(user);
			persist(contentDefinition, deliveryModel, publicationUserId,
					original, publicationContentPersister, result);
			ctx.getVisitorOrNoop()
					.afterPublicationPersistence(result.publicationId);
		}
		long publicationId = CommonUtils.lv(result.publicationId);
		ContentRenderer crh = (ContentRenderer) Registry.get()
				.instantiateSingle(ContentRenderer.class,
						publicationContent.getClass());
		crh.renderContent(contentDefinition, publicationContent, deliveryModel,
				publicationId, publicationUserId);
		ctx.renderedContent = crh.results;
		if (crh.getResults().persist && publicationContentPersister != null
				&& publicationId != 0) {
			publicationContentPersister.persistContentRendererResults(
					crh.getResults(), publicationId);
		}
		ContentWrapper cw = (ContentWrapper) Registry.get().instantiateSingle(
				ContentWrapper.class, publicationContent.getClass());
		ctx.getVisitorOrNoop().beforeWrapContent();
		cw.wrapContent(contentDefinition, publicationContent, deliveryModel,
				crh.getResults(), publicationId, publicationUserId);
		ctx.getVisitorOrNoop().afterWrapContent(cw);
		if (deliveryModel.provideContentDeliveryType().getClass() == null) {
			return null;
		}
		result.content = cw.wrappedContent;
		if (deliveryModel
				.provideContentDeliveryType() == ContentDeliveryType.PRINT) {
			if (result.content == null & AppPersistenceBase.isTest()) {
				result.content = Base64Utils.toBase64(cw.wrappedBytes);
			}
			return result;
		}
		FormatConverter fc = (FormatConverter) Registry.get().instantiateSingle(
				FormatConverter.class,
				deliveryModel.provideTargetFormat().getClass());
		FormatConversionModel fcm = new FormatConversionModel();
		fcm.html = cw.wrappedContent;
		fcm.footer = cw.wrappedFooter;
		fcm.bytes = cw.wrappedBytes;
		fcm.rows = cw.wrapper.gridRows;
		fcm.custom = cw.custom;
		ctx.formatConversionModel = fcm;
		InputStream convertedContent = fc.convert(ctx, fcm);
		convertedContent = ctx.getVisitorOrNoop()
				.transformConvertedContent(convertedContent);
		ctx.getVisitorOrNoop().beforeDelivery();
		ContentDelivery deliverer = (ContentDelivery) Registry.get()
				.instantiateSingle(ContentDeliveryType.class,
						deliveryModel.provideContentDeliveryType().getClass());
		String token = deliverer.deliver(ctx, convertedContent, deliveryModel,
				fc);
		if (forPublication && publicationContentPersister != null
				&& !AppPersistenceBase.isInstanceReadOnly()) {
			postDeliveryPersistence(publicationId);
			crh.getResults().htmlContent = cw.wrappedContent;
			persist(contentDefinition, deliveryModel, publicationUserId,
					original, publicationContentPersister, result);
			if (crh.getResults().persist && publicationContentPersister != null
					&& result.publicationId != 0) {
				publicationContentPersister.persistContentRendererResults(
						crh.getResults(), result.publicationId);
			}
		}
		result.content = null;
		result.contentToken = token;
		ctx.getVisitorOrNoop().publicationFinished(result);
		return result;
	}

	public interface PublicationContentPersister {
		public ContentRendererResults
				getContentRendererResults(long publicationId);

		public Publication newPublicationInstance();

		public void persistContentRendererResults(
				ContentRendererResults results, long publicationId);
	}
}
