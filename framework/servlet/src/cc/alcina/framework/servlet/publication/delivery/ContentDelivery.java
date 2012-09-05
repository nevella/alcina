package cc.alcina.framework.servlet.publication.delivery;

import java.io.InputStream;

import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.servlet.publication.FormatConverter;

public interface ContentDelivery {
	public String deliver(InputStream convertedContent,
			DeliveryModel deliveryModel, FormatConverter hfc)
			throws Exception;
}
