package cc.alcina.framework.servlet.publication.delivery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_DOWNLOAD_PREVIEW;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.servlet.DownloadServlet;
import cc.alcina.framework.servlet.servlet.DownloadServlet.DownloadItem;

@RegistryLocation(registryPoint = ContentDeliveryType.class, targetClass = ContentDeliveryType_DOWNLOAD_PREVIEW.class)
public class ContentDeliveryDownloadInline implements ContentDelivery {
	protected String deliverViaServlet(InputStream stream, String mimeType,
			String suggestedFileName, String suffix) throws Exception {
		File file = File.createTempFile(suggestedFileName, "." + suffix);
		file.deleteOnExit();
		ResourceUtilities.writeStreamToStream(stream,
				new FileOutputStream(file));
		DownloadItem item = new DownloadServlet.DownloadItem(mimeType,
				null, file.getPath());
		return DownloadServlet.add(item);
	}

	public String deliver(InputStream convertedContent,
			DeliveryModel deliveryModel, FormatConverter hfc)
			throws Exception {
		return deliverViaServlet(convertedContent, hfc.getMimeType(),
				deliveryModel.getSuggestedFileName(), hfc.getFileExtension());
	}
}
