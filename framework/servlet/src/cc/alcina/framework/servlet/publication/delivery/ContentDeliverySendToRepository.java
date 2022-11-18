package cc.alcina.framework.servlet.publication.delivery;

import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_SEND_TO_REPOSITORY;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.repository.ContentRepository;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@Registration({ ContentDeliveryType.class,
		ContentDeliveryType_SEND_TO_REPOSITORY.class })
public class ContentDeliverySendToRepository implements ContentDelivery {
	@Override
	public synchronized String deliver(PublicationContext ctx,
			InputStream convertedContent, DeliveryModel deliveryModel,
			FormatConverter hfc) throws Exception {
		ContentRepository repository = ContentRepository
				.forConnection(deliveryModel.getRepositoryConnection());
		repository.put(deliveryModel.getSuggestedFileName(), convertedContent);
		return null;
	}
}
