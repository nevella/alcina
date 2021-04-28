package cc.alcina.framework.servlet.publication;

import java.io.InputStream;
import java.util.Date;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.publication.Publication.Definition;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.request.NonRootPublicationRequest;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.util.MethodContext;
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
 * @author nick@alcina.cc
 *
 */
public class Publisher {
	public static final String CONTEXT_SAVE_BYTES_TO_PRINT_CONTENT = Publisher.class
			.getName() + ".CONTEXT_SAVE_BYTES_TO_PRINT_CONTENT";

	public static boolean useWrappedObjectSerialization() {
		return ResourceUtilities.is(Publisher.class,
				"useWrappedObjectSerialization");
	}

	private PublicationContext ctx;

	public PublicationContext getContext() {
		return this.ctx;
	}

	public PublicationResult publish(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel) throws Exception {
		return publish(contentDefinition, deliveryModel, null);
	}

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
			Publication original, PublicationResult result) {
		PublicationPersister persister = PublicationPersister.get();
		ctx.publication = persister.newPublicationInstance();
		Publication publication = ctx.publication;
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
		if (useWrappedObjectSerialization()) {
			publication.setContentDefinition(contentDefinition);
			publication.setDeliveryModel(deliveryModel);
		} else {
			Definition definition = deliveryModel.provideDefinition();
			Preconditions.checkArgument(definition
					.provideContentDefinition() == contentDefinition
					&& definition.provideDeliveryModel() == deliveryModel);
			publication.setDefinition(definition);
			publication.setDefinitionDescription(definition.toString());
		}
		publication.setUser(PermissionsManager.get().getUser());
		publication.setPublicationDate(new Date());
		publication.setOriginalPublication(original);
		publication.setUserPublicationId(publicationUserId);
		publication.setPublicationUid(SEUtilities.generateId());
		publication.setPublicationType(contentDefinition.getPublicationType());
		persister.persist(publication);
		result.publicationId = ctx.publication.getId();
		result.publicationUid = publication.getPublicationUid();
	}

	private void postDeliveryPersistence(Long publicationId) {
		if (getContext().mimeMessageId != null) {
			Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence().updatePublicationMimeMessageId(
							publicationId, getContext().mimeMessageId);
		}
	}

	private PublicationResult publish0(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Publication original)
			throws Exception {
		if (deliveryModel instanceof NonRootPublicationRequest
				&& PermissionsManager.get().isRoot()) {
			throw Ax.runtimeException(
					"Publication %s cannot be published as root",
					deliveryModel);
		}
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
		PublicationContent publicationContent = cmh.getPublicationContent();
		ctx.publicationContent = publicationContent;
		result.publicationUid = deliveryModel.getPublicationUid();
		PublicationPersister persister = PublicationPersister.get();
		boolean persistPublication = forPublication
				&& !AppPersistenceBase.isInstanceReadOnly();
		if (persistPublication) {
			IUser user = PermissionsManager.get().getUser();
			if (user == null) {
				user = UserlandProvider.get().getSystemUser();
			}
			publicationUserId = persister.getNextPublicationIdForUser(user);
			persist(contentDefinition, deliveryModel, publicationUserId,
					original, result);
			ctx.getVisitorOrNoop()
					.afterPublicationPersistence(result.publicationId);
		} else {
			if (result.publicationUid == null) {
				result.publicationUid = SEUtilities.generateId();
				result.publicationId = 0L;
			}
		}
		long publicationId = CommonUtils.lv(result.publicationId);
		ContentRenderer crh = (ContentRenderer) Registry.get()
				.instantiateSingle(ContentRenderer.class,
						publicationContent.getClass());
		ctx.getVisitorOrNoop().beforeRenderContent();
		publicationContent = ctx.publicationContent;
		crh.renderContent(contentDefinition, publicationContent, deliveryModel,
				publicationId, publicationUserId);
		ctx.renderedContent = crh.results;
		if (crh.getResults().persist && ctx.publication != null) {
			persister.persistContentRendererResults(crh.getResults(),
					ctx.publication);
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
			if (result.content == null && (AppPersistenceBase.isTest()
					|| LooseContext.is(CONTEXT_SAVE_BYTES_TO_PRINT_CONTENT))) {
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
		fcm.stream = cw.stream;
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
		if (persistPublication) {
			postDeliveryPersistence(publicationId);
			crh.getResults().htmlContent = cw.wrappedContent;
			if (crh.getResults().persist && ctx.publication != null) {
				persister.persistContentRendererResults(crh.getResults(),
						ctx.publication);
			}
		}
		result.content = null;
		result.contentToken = token;
		ctx.getVisitorOrNoop().publicationFinished(result);
		return result;
	}

	@RegistryLocation(registryPoint = PublicationPersister.class, implementationType = ImplementationType.SINGLETON)
	public static class PublicationPersister {
		public static Publisher.PublicationPersister get() {
			return Registry.impl(Publisher.PublicationPersister.class);
		}

		public ContentRendererResults
				getContentRendererResults(Publication publication) {
			if (publication.getSerializedPublication() != null) {
				return KryoUtils.deserializeFromBase64(
						publication.domain().ensurePopulated()
								.getSerializedPublication(),
						ContentRendererResults.class);
			}
			return null;
		}

		public long getNextPublicationIdForUser(IUser user) {
			boolean wasMuted = MetricLogging.get().isMuted();
			try {
				MetricLogging.get().setMuted(false);
				return MethodContext.instance()
						.withMetricKey(
								"publication-getNextPublicationIdForUser")
						.call(() -> CommonPersistenceProvider.get()
								.getCommonPersistence()
								.getNextPublicationIdForUser(user));
			} finally {
				MetricLogging.get().setMuted(wasMuted);
			}
		}

		public Publication newPublicationInstance() {
			if (useWrappedObjectSerialization()) {
				return PersistentImpl
						.getNewImplementationInstance(Publication.class);
			} else {
				return PersistentImpl.create(Publication.class);
			}
		}

		public void persist(Publication publication) {
			if (useWrappedObjectSerialization()) {
				Publication merged = Registry
						.impl(CommonPersistenceProvider.class)
						.getCommonPersistence().merge(publication);
				ResourceUtilities.copyBeanProperties(merged, publication, null,
						false);
			} else {
				Transaction.commit();
			}
		}

		public void persistContentRendererResults(
				ContentRendererResults results, Publication publication) {
			publication.setSerializedPublication(
					KryoUtils.serializeToBase64(results));
			if (useWrappedObjectSerialization()) {
				Publication merged = Registry
						.impl(CommonPersistenceProvider.class)
						.getCommonPersistence().merge(publication);
				ResourceUtilities.copyBeanProperties(merged, publication, null,
						false);
				publication
						.setVersionNumber(publication.getVersionNumber() + 1);
			} else {
				Transaction.commit();
			}
		}
	}
}
