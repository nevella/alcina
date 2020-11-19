package cc.alcina.framework.servlet.publication.delivery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_SEND_TO_REPOSITORY;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

public abstract class ContentDeliveryRepositoryHandler implements ContentDelivery {
	public abstract  String deliver(PublicationContext ctx,
			InputStream convertedContent, DeliveryModel deliveryModel,
			FormatConverter hfc) throws Exception ;
	
}
