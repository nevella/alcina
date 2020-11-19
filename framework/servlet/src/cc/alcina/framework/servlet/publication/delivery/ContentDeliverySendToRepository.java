package cc.alcina.framework.servlet.publication.delivery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_SEND_TO_REPOSITORY;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.RepositoryDelivery;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

@RegistryLocation(registryPoint = ContentDeliveryType.class, targetClass = ContentDeliveryType_SEND_TO_REPOSITORY.class)
public class ContentDeliverySendToRepository implements ContentDelivery {
	public synchronized String deliver(PublicationContext ctx,
			InputStream convertedContent, DeliveryModel deliveryModel,
			FormatConverter hfc) throws Exception {
		String repositoryMarkerClassName = deliveryModel.getProperties().get(RepositoryDelivery.PUBLICATION_PROPERTY_REPOSITORY_CLASS);
		Class<? extends RepositoryDelivery> deliveryClass =  (Class<? extends RepositoryDelivery>) Class.forName(repositoryMarkerClassName);
		ContentDeliveryRepositoryHandler repositoryHandler = Registry.impl(ContentDeliveryRepositoryHandler.class,deliveryClass);
		return repositoryHandler.deliver(ctx, convertedContent, deliveryModel, hfc);
	}
}
