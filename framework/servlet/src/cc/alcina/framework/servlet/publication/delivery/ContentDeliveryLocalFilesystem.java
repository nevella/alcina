package cc.alcina.framework.servlet.publication.delivery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_LOCAL_FILESYSTEM;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.HasLocalDelivery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

/**
 * 
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = ContentDeliveryType.class, targetClass = ContentDeliveryType_LOCAL_FILESYSTEM.class)
public class ContentDeliveryLocalFilesystem implements ContentDelivery {
	public String deliver(PublicationContext ctx, InputStream convertedContent,
			DeliveryModel deliveryModel, FormatConverter hfc) throws Exception {
		HasLocalDelivery hld = (HasLocalDelivery) PublicationContext
				.get().contentDefinition;
		String suggestedFileName = deliveryModel.getSuggestedFileName();
		new File(hld.provideLocalDeliveryFolder()).mkdirs();
		File file = new File(Ax.format("%s/%s",
				hld.provideLocalDeliveryFolder(), suggestedFileName));
		ResourceUtilities.writeStreamToStream(convertedContent,
				new FileOutputStream(file));
		return file.getPath();
	}
}
