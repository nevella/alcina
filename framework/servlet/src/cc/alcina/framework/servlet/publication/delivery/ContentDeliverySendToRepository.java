package cc.alcina.framework.servlet.publication.delivery;

import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_SEND_TO_REPOSITORY;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.RepositoryDelivery;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@RegistryLocation(registryPoint = ContentDeliveryType.class, targetClass = ContentDeliveryType_SEND_TO_REPOSITORY.class)
@Registration({ ContentDeliveryType.class,
		ContentDeliveryType_SEND_TO_REPOSITORY.class })
public class ContentDeliverySendToRepository implements ContentDelivery {
	@Override
	public synchronized String deliver(PublicationContext ctx,
			InputStream convertedContent, DeliveryModel deliveryModel,
			FormatConverter hfc) throws Exception {
		String repositoryMarkerClassName = deliveryModel.getProperties()
				.get(RepositoryDelivery.PUBLICATION_PROPERTY_REPOSITORY_CLASS);
		Class<? extends RepositoryDelivery> deliveryClass = (Class<? extends RepositoryDelivery>) Class
				.forName(repositoryMarkerClassName);
		ContentDeliveryRepositoryHandler repositoryHandler = Registry
				.query(ContentDeliveryRepositoryHandler.class)
				.addKeys(deliveryClass).impl();
		return repositoryHandler.deliver(ctx, convertedContent, deliveryModel,
				hfc);
	}
}
