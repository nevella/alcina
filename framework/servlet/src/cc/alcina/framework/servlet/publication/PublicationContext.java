package cc.alcina.framework.servlet.publication;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.persistence.WrappedObject.WrappedObjectHelper;
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

	public static PublicationContext setupForExternalToPublisher(
			ContentDefinition contentDefinition, DeliveryModel deliveryModel) {
		return setupContext(contentDefinition, deliveryModel);
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

	public String getContextInfoForPublicationException() {
		String xmlForm = "Unable to serialize publication request";
		String modelString = xmlForm;
		try {
			Set<Class> jaxbClasses = new HashSet<Class>(
					Registry.get().lookup(JaxbContextRegistration.class));
			xmlForm = Ax.format(
					"Content definition:\n%s\n\n" + "Delivery model:\n%s",
					WrappedObjectHelper.xmlSerialize(contentDefinition,
							jaxbClasses),
					WrappedObjectHelper.xmlSerialize(deliveryModel,
							jaxbClasses));
			if (xmlForm.length() > 5000) {
				xmlForm = Ax.format(
						"(Large definition/model)\n"
								+ "Content definition: %s (%s chars)\n"
								+ "Delivery model: %s (%s chars)",
						contentDefinition.getClass().getSimpleName(),
						WrappedObjectHelper
								.xmlSerialize(contentDefinition, jaxbClasses)
								.length(),
						deliveryModel.getClass().getSimpleName(),
						WrappedObjectHelper
								.xmlSerialize(deliveryModel, jaxbClasses)
								.length());
			}
			modelString = deliveryModel.toString();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		String message = String.format("Publication exception: %s %s\n%s",
				PermissionsManager.get().getUserName(), modelString, xmlForm);
		return message;
	}

	public PublicationVisitor getVisitor() {
		return this.visitor;
	}

	public PublicationVisitor getVisitorOrNoop() {
		return this.visitor == null ? new PublicationVisitor() : this.visitor;
	}

	public void setVisitor(PublicationVisitor visitor) {
		this.visitor = visitor;
	}

	protected void logPublicationException(Exception e) {
		String message = getContextInfoForPublicationException();
		logger.warn(message, e);
		EntityLayerLogging.log(LogMessageType.PUBLICATION_EXCEPTION, message,
				e);
	}
}
