package cc.alcina.framework.servlet.publication;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.entityaccess.WrappedObject.WrappedObjectHelper;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.servlet.ServletLayerRegistry;

public class PublicationContext {
	public static final String CONTEXT_PUBLICATION_CONTEXT = PublicationContext.class
			.getName() + ".CONTEXT_PUBLICATION_CONTEXT";

	public ContentDefinition contentDefinition;

	public PublicationContent publicationContent;

	public DeliveryModel deliveryModel;

	public PublicationResult publicationResult;

	public Map<String, Object> properties = new LinkedHashMap<String, Object>();
	
	public PublicationVisitor visitor;

	public Logger logger;

	public String getContextInfoForPublicationException() {
		String xmlForm = "Unable to serialize publication request";
		String modelString=xmlForm;
		try {
			Set<Class> jaxbClasses = new HashSet<Class>(ServletLayerRegistry
					.get().lookup(JaxbContextRegistration.class));
			xmlForm = String.format("Content definition:\n%s\n\n"
					+ "Delivery model:\n%s", WrappedObjectHelper.xmlSerialize(
					contentDefinition, jaxbClasses), WrappedObjectHelper
					.xmlSerialize(deliveryModel, jaxbClasses));
			modelString=deliveryModel.toString();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		String message = String.format("Publication exception: %s %s\n%s",
				PermissionsManager.get().getUserName(), modelString, xmlForm);
		return message;
	}

	protected void logPublicationException(Exception e) {
		String message = getContextInfoForPublicationException();
		logger.warn(message, e);
		EntityLayerLocator.get().log(LogMessageType.PUBLICATION_EXCEPTION,
				message, e);
	}

	public static String getContextInfoForPublicationExceptionT() {
		PublicationContext ctx = get();
		return ctx == null ? "--no publication context--" : ctx
				.getContextInfoForPublicationException();
	}

	public static PublicationContext get() {
		return LooseContext.get(CONTEXT_PUBLICATION_CONTEXT);
	}

	public PublicationVisitor getVisitor() {
		return this.visitor;
	}

	public void setVisitor(PublicationVisitor visitor) {
		this.visitor = visitor;
	}
}
