package cc.alcina.framework.servlet.publication;

import java.io.InputStream;
import java.util.Date;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_PRINT;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.ServletLayerLocator;
import cc.alcina.framework.servlet.ServletLayerRegistry;
import cc.alcina.framework.servlet.publication.ContentRenderer.ContentRendererResults;
import cc.alcina.framework.servlet.publication.FormatConverter.FormatConversionModel;
import cc.alcina.framework.servlet.publication.PublicationPersistence.PublicationPersistenceLocator;
import cc.alcina.framework.servlet.publication.delivery.ContentDelivery;

/**
 * <p>Process a content definition and deliver it to a delivery model (e.g. email a list of documents, convert a document to pdf and serve etc)
 * </p>
 * <h3>Pipeline</h3>
 * <ol>
 * <li>ContentModelHandler: convert a content definition (say a search description) to a list of objects (say search results)
 * <li>ContentRenderer: convert the list of objects to an output format (most often xhtml)
 * <li>ContentWrapper: wrap content (add header, footer, that sort of decoration)
 * <li>FormatConverter: at this stage output is either html or some binary format - if html, can convert to Word, PDF etc
 * <li>ContentDelivery: deliver (email, download etc)
 * </ol>
 * 
 * 
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class Publisher {
	public PublicationResult publish(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel) throws Exception {
		return publish(contentDefinition, deliveryModel, null);
	}

	@SuppressWarnings("unchecked")
	public PublicationResult publish(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Publication original) throws Exception {
		ContentModelHandler cmh = (ContentModelHandler) ServletLayerRegistry
				.get().instantiateSingle(ContentModelHandler.class,
						contentDefinition.getClass());
		cmh.prepareContent(contentDefinition, deliveryModel);
		if (!cmh.hasResults) {
			return null;// throw exception??
		}
		PublicationResult result = new PublicationResult();
		long publicationUserId = 0;
		long publicationId = 0;
		boolean forPublication = !deliveryModel.isNoPersistence();
		if (!SEUtilities.localTestMode()) {
			if (forPublication && publicationPersister != null) {
				publicationUserId = PublicationPersistenceLocator
						.get()
						.publicationPersistence()
						.getNextPublicationIdForUser(
								PermissionsManager.get().getUser());
				publicationId = persist(contentDefinition, deliveryModel,
						publicationUserId, original);
				result.publicationId = publicationId;
			}
		}
		PublicationContent publicationContent = cmh.getPublicationContent();
		ContentRenderer crh = (ContentRenderer) ServletLayerRegistry.get()
				.instantiateSingle(ContentRenderer.class,
						publicationContent.getClass());
		crh.renderContent(contentDefinition, publicationContent, deliveryModel,
				publicationId, publicationUserId);
		if (!SEUtilities.localTestMode() && crh.getResults().persist
				&& publicationPersister != null && publicationId != 0) {
			publicationPersister.persistContentRendererResults(
					crh.getResults(), publicationId);
		}
		ContentWrapper cw = (ContentWrapper) ServletLayerRegistry.get()
				.instantiateSingle(ContentWrapper.class,
						publicationContent.getClass());
		cw.wrapContent(contentDefinition, publicationContent, deliveryModel,
				crh.getResults(), publicationId, publicationUserId);
		if (deliveryModel.provideContentDeliveryType().getClass() == null) {
			return null;
		}
		result.content = cw.wrappedContent;
		if (deliveryModel.provideContentDeliveryType() == ContentDeliveryType.PRINT) {
			return result;
		}
		FormatConverter fc = (FormatConverter) ServletLayerRegistry.get()
				.instantiateSingle(FormatConverter.class,
						deliveryModel.provideTargetFormat().getClass());
		FormatConversionModel fcm = new FormatConversionModel();
		fcm.html = cw.wrappedContent;
		fcm.footer = cw.wrappedFooter;
		fcm.bytes = cw.wrappedBytes;
		fcm.custom=cw.custom;
		InputStream convertedContent = fc.convert(fcm);
		ContentDelivery deliverer = (ContentDelivery) ServletLayerRegistry
				.get().instantiateSingle(ContentDeliveryType.class,
						deliveryModel.provideContentDeliveryType().getClass());
		String token = deliverer.deliver(convertedContent, deliveryModel, fc);
		result.content=null;
		result.contentToken=token;
		return result;
	}

	private long persist(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Long publicationUserId,
			Publication original) {
		Publication publication = publicationPersister.newPublicationInstance();
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
		return ServletLayerLocator.get().commonPersistenceProvider()
				.getCommonPersistence().merge(publication);
	}

	private static PublicationPersister publicationPersister;

	public static void registerPublicationPersister(PublicationPersister pp) {
		publicationPersister = pp;
	}

	public interface PublicationPersister {
		public Publication newPublicationInstance();

		public void persistContentRendererResults(
				ContentRendererResults results, long publicationId);

		public ContentRendererResults getContentRendererResults(
				long publicationId);
	}
}
