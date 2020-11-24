package cc.alcina.framework.servlet.publication.delivery;

import java.io.InputStream;

import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

public abstract class ContentDeliveryRepositoryHandler implements ContentDelivery {
	public abstract  String deliver(PublicationContext ctx,
			InputStream convertedContent, DeliveryModel deliveryModel,
			FormatConverter hfc) throws Exception ;
	
}
