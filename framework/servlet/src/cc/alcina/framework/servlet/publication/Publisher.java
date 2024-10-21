package cc.alcina.framework.servlet.publication;

import java.io.InputStream;
import java.util.Date;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.publication.Publication.Definition;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.NonRootPublicationRequest;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
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
 */
public class Publisher {
	public static final String CONTEXT_SAVE_BYTES_TO_PRINT_CONTENT = Publisher.class
			.getName() + ".CONTEXT_SAVE_BYTES_TO_PRINT_CONTENT";

	public static final String CONTEXT_PERSIST_CONTENT_RENDERER_RESULTS = Publisher.class
			.getName() + ".CONTEXT_PERSIST_CONTENT_RENDERER_RESULTS";

	private PublicationContext context;

	private ContentWrapper contentWrapper;

	private PublicationResult result;

	private InputStream convertedContent;

	private boolean exitPreDelivery() {
		if (context.deliveryModel
				.provideContentDeliveryType() == ContentDeliveryType.PRINT) {
			return true;
		} else {
			return false;
		}
	}

	public PublicationContext getContext() {
		return this.context;
	}

	private void persist(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel, Long publicationUserId,
			Publication original, PublicationResult result) {
		PublicationPersister persister = PublicationPersister.get();
		context.publication = persister.newPublicationInstance();
		Publication publication = context.publication;
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
		Definition definition = deliveryModel.provideDefinition();
		if (definition.provideContentDefinition() == null) {
			((ContentRequestBase) deliveryModel)
					.setContentDefinition(contentDefinition);
		}
		Preconditions.checkArgument(
				definition.provideContentDefinition() == contentDefinition
						&& definition.provideDeliveryModel() == deliveryModel);
		publication.setDefinition(definition);
		publication.setDefinitionDescription(definition.toString());
		publication.setUser(PermissionsManager.get().getUser());
		publication.setPublicationDate(new Date());
		publication.setOriginalPublication(original);
		publication.setUserPublicationId(publicationUserId);
		publication.setPublicationUid(SEUtilities.generateId());
		publication.setPublicationType(contentDefinition.getPublicationType());
		result.setPublicationId(persister.persist(publication));
		result.setPublicationUid(publication.getPublicationUid());
	}

	private void postDeliveryPersistence(Long publicationId) {
		if (getContext().mimeMessageId != null) {
			Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence().updatePublicationMimeMessageId(
							publicationId, getContext().mimeMessageId);
		}
	}

	private void preparePreDeliveryExitResult() {
		if (convertedContent != null) {
			result.setContent(Base64Utils.toBase64(
					Io.read().fromStream(convertedContent).asBytes()));
		} else if (result.getContent() == null && (AppPersistenceBase.isTest()
				|| LooseContext.is(CONTEXT_SAVE_BYTES_TO_PRINT_CONTENT))) {
			result.setContent(
					Base64Utils.toBase64(contentWrapper.wrappedBytes));
		}
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
			context = PublicationContext.setupContext(contentDefinition,
					deliveryModel);
			LooseContext.pushWithKey(
					PublicationContext.CONTEXT_PUBLICATION_CONTEXT, context);
			return publish0(contentDefinition, deliveryModel, original);
		} catch (Exception e) {
			context.logPublicationException(e);
			throw e;
		} finally {
			LooseContext.pop();
			LooseContext.confirmDepth(depth);
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
		ContentModelHandler cmh = Registry.impl(ContentModelHandler.class,
				contentDefinition.getClass());
		cmh.prepareContent(contentDefinition, deliveryModel);
		PublicationVisitor visitor = context.getVisitorOrNoop();
		visitor.afterPrepareContent(cmh);
		if (!cmh.hasResults) {
			// throw exception??
			return null;
		}
		result = new PublicationResult();
		context.publicationResult = result;
		long publicationUserId = 0;
		boolean forPublication = !deliveryModel.isNoPersistence()
				&& deliveryModel.provideContentDeliveryType().isRepublishable();
		PublicationContent publicationContent = cmh.getPublicationContent();
		context.publicationContent = publicationContent;
		result.setPublicationUid(deliveryModel.getPublicationUid());
		PublicationPersister persister = PublicationPersister.get();
		boolean persistPublication = forPublication
				&& !AppPersistenceBase.isInstanceReadOnly();
		if (persistPublication) {
			IUser user = PermissionsManager.get().getUser();
			if (user == null) {
				user = UserlandProvider.get().getSystemUser();
			}
			ClientInstance clientInstance = ClientInstance.self();
			if (clientInstance == null) {
				clientInstance = EntityLayerObjects.get()
						.getServerAsClientInstance();
			}
			if (Configuration.is("trackPublicationClientInstanceId")) {
				deliveryModel
						.setRequestorClientInstanceId(clientInstance.getId());
			}
			publicationUserId = persister.getNextPublicationIdForUser(user);
			persist(contentDefinition, deliveryModel, publicationUserId,
					original, result);
			visitor.afterPublicationPersistence(result.getPublicationId());
		} else {
			if (result.getPublicationUid() == null) {
				result.setPublicationUid(SEUtilities.generateId());
				result.setPublicationId(0L);
			}
		}
		long publicationId = CommonUtils.lv(result.getPublicationId());
		ContentRenderer contentRenderer = Registry.impl(ContentRenderer.class,
				publicationContent.getClass());
		visitor.beforeRenderContent();
		publicationContent = context.publicationContent;
		contentRenderer.renderContent(contentDefinition, publicationContent,
				deliveryModel, publicationId, publicationUserId);
		context.renderedContent = contentRenderer.results;
		if (contentRenderer.getResults().persist
				&& context.publication != null) {
			persister.persistContentRendererResults(
					contentRenderer.getResults(), context.publication);
		}
		contentWrapper = Registry.impl(ContentWrapper.class,
				publicationContent.getClass());
		visitor.beforeWrapContent();
		contentWrapper.wrapContent(contentDefinition, publicationContent,
				deliveryModel, contentRenderer.getResults(), publicationId,
				publicationUserId);
		visitor.afterWrapContent(contentWrapper);
		if (deliveryModel.provideContentDeliveryType().getClass() == null) {
			return null;
		}
		result.setContent(contentWrapper.wrappedContent);
		FormatConverter formatConverter = Registry.impl(FormatConverter.class,
				deliveryModel.provideTargetFormat().getClass());
		if (formatConverter.isPassthrough() && exitPreDelivery()) {
			preparePreDeliveryExitResult();
			return result;
		}
		FormatConversionModel formatConversionModel = new FormatConversionModel();
		formatConversionModel.html = contentWrapper.wrappedContent;
		formatConversionModel.footer = contentWrapper.wrappedFooter;
		formatConversionModel.bytes = contentWrapper.wrappedBytes;
		formatConversionModel.stream = contentWrapper.stream;
		formatConversionModel.rows = contentWrapper.wrapper.gridRows;
		formatConversionModel.custom = contentWrapper.custom;
		context.formatConversionModel = formatConversionModel;
		formatConversionModel.fileExtension = formatConverter
				.getFileExtension();
		formatConversionModel.mimeType = formatConverter.getMimeType();
		convertedContent = formatConverter.convert(context,
				formatConversionModel);
		convertedContent = visitor.transformConvertedContent(convertedContent);
		if (exitPreDelivery()) {
			preparePreDeliveryExitResult();
			return result;
		}
		visitor.beforeDelivery();
		ContentDelivery deliverer = Registry.query(ContentDelivery.class)
				.setKeys(ContentDeliveryType.class,
						deliveryModel.provideContentDeliveryType().getClass())
				.impl();
		String token = deliverer.deliver(context, convertedContent,
				deliveryModel, formatConverter);
		if (persistPublication) {
			postDeliveryPersistence(publicationId);
			contentRenderer
					.getResults().htmlContent = contentWrapper.wrappedContent;
			if ((LooseContext.is(CONTEXT_PERSIST_CONTENT_RENDERER_RESULTS)
					|| contentRenderer.getResults().persist)
					&& context.publication != null) {
				persister.persistContentRendererResults(
						contentRenderer.getResults(), context.publication);
			}
		}
		result.setContent(null);
		if (deliverer.returnsDownloadToken()) {
			result.setContentToken(token);
		}
		visitor.publicationFinished(result);
		return result;
	}

	@Registration.Singleton
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
			synchronized (user) {
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
		}

		public Publication newPublicationInstance() {
			return PersistentImpl.create(Publication.class);
		}

		public long persist(Publication publication) {
			if (TransformCommit.isCommitting()) {
				return 0L;
			}
			return TransformCommit.commitTransformsAndReturnId(true,
					publication);
		}

		public void persistContentRendererResults(
				ContentRendererResults results, Publication publication) {
			if (TransformCommit.isCommitting()) {
				return;
			}
			publication.setSerializedPublication(
					KryoUtils.serializeToBase64(results));
			Transaction.commit();
		}
	}

	public static class ResultLoggerImpl
			implements PublicationResult.ResultLogger {
		@Override
		public void log(PublicationResult publicationResult) {
			Io.log().toFile(publicationResult.getContent());
		}
	}
}
