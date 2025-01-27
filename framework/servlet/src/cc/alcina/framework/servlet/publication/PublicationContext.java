package cc.alcina.framework.servlet.publication;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.Publication;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.util.JaxbUtils;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.servlet.publication.ContentRenderer.ContentRendererResults;
import cc.alcina.framework.servlet.publication.FormatConverter.FormatConversionModel;

public class PublicationContext {
	public static final String CONTEXT_PUBLICATION_CONTEXT = PublicationContext.class
			.getName() + ".CONTEXT_PUBLICATION_CONTEXT";

	public static final String CONTEXT_PUBLICATION_VISITOR = PublicationContext.class
			.getName() + ".CONTEXT_PUBLICATION_VISITOR";

	public static PublicationContext get() {
		return LooseContext.get(CONTEXT_PUBLICATION_CONTEXT);
	}

	public static String getContextInfoForPublicationExceptionT() {
		PublicationContext ctx = get();
		return ctx == null ? "--no publication context--"
				: ctx.getContextInfoForPublicationException();
	}

	public static boolean has() {
		return LooseContext.has(CONTEXT_PUBLICATION_CONTEXT);
	}

	static PublicationContext setupContext(ContentDefinition contentDefinition,
			DeliveryModel deliveryModel) {
		PublicationContext ctx = new PublicationContext();
		ctx.logger = Logger.getLogger(Publisher.class);
		ctx.contentDefinition = contentDefinition;
		ctx.deliveryModel = deliveryModel;
		if (LooseContext.has(CONTEXT_PUBLICATION_VISITOR)) {
			ctx.setVisitor(LooseContext.get(CONTEXT_PUBLICATION_VISITOR));
		}
		return ctx;
	}

	public static PublicationContext setupForExternalToPublisher(
			ContentDefinition contentDefinition, DeliveryModel deliveryModel) {
		return setupContext(contentDefinition, deliveryModel);
	}

	public ContentDefinition contentDefinition;

	public PublicationContent publicationContent;

	public DeliveryModel deliveryModel;

	public PublicationResult publicationResult;

	public Map<String, Object> properties = new LinkedHashMap<String, Object>();

	public PublicationVisitor visitor;

	public Logger logger;

	public String mimeMessageId;

	public ContentRendererResults renderedContent;

	public FormatConversionModel formatConversionModel;

	public Publication publication;

	public String getContextInfoForPublicationException() {
		String jsonForm = "Unable to serialize publication request";
		String modelString = jsonForm;
		try {
			String contentDefSerialized = "(no xmlrootelement annotation)";
			contentDefSerialized = ReflectiveSerializer
					.serialize(contentDefinition);
			jsonForm = Ax.format(
					"Content definition:\n%s\n\n" + "Delivery model:\n%s",
					contentDefSerialized,
					ReflectiveSerializer.serialize(deliveryModel));
			if (jsonForm.length() > 5000) {
				jsonForm = Ax.format(
						"(Large definition/model)\n"
								+ "Content definition: %s (%s chars)\n"
								+ "Delivery model: %s (%s chars)",
						contentDefinition.getClass().getSimpleName(),
						contentDefSerialized.length(),
						deliveryModel.getClass().getSimpleName(),
						JaxbUtils.xmlSerialize(deliveryModel).length());
			}
			modelString = MethodContext.instance().withWrappingTransaction()
					.call(() -> deliveryModel.toString());
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		String message = String.format("Publication exception: %s %s\n%s",
				PermissionsManager.get().getUserName(), modelString, jsonForm);
		return message;
	}

	public PublicationVisitor getVisitor() {
		return this.visitor;
	}

	public PublicationVisitor getVisitorOrNoop() {
		return this.visitor == null ? new PublicationVisitor() : this.visitor;
	}

	protected void logPublicationException(Exception e) {
		String message = getContextInfoForPublicationException();
		logger.warn(message, e);
		EntityLayerLogging.log(LogMessageType.PUBLICATION_EXCEPTION, message,
				e);
	}

	public void setVisitor(PublicationVisitor visitor) {
		this.visitor = visitor;
	}
}
