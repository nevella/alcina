package cc.alcina.framework.servlet.publication;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.PublicationContent;
import cc.alcina.framework.common.client.publication.request.PublicationResult;

public class PublicationContext {
	public ContentDefinition contentDefinition;

	public PublicationContent publicationContent;

	public DeliveryModel deliveryModel;

	public PublicationResult publicationResult;

	public Map<String, Object> properties = new LinkedHashMap<String, Object>();
}
