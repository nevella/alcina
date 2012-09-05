package au.com.barnet.common.j2se.server.publication.delivery;

import java.io.InputStream;

import au.com.barnet.common.j2se.server.publication.FormatConverter;
import cc.alcina.framework.common.client.publication.DeliveryModel;

public interface ContentDelivery {
	public String deliver(InputStream convertedContent,
			DeliveryModel deliveryModel, FormatConverter hfc)
			throws Exception;
}
