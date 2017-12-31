package cc.alcina.framework.servlet.publication.delivery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_DOWNLOAD_ATTACHMENT;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;
import cc.alcina.framework.servlet.servlet.DownloadServlet;
import cc.alcina.framework.servlet.servlet.DownloadServlet.DownloadItem;

/**
 * could extend xxxMimeType - but we'd need to expand the registry, with a
 * "no-inherit"..TODO??
 * 
 * @author nreddel@barnet.com.au
 *
 */
@RegistryLocation(registryPoint = ContentDeliveryType.class, targetClass = ContentDeliveryType_DOWNLOAD_ATTACHMENT.class)
public class ContentDeliveryDownloadAsAttachment implements ContentDelivery {
	public String deliver(PublicationContext ctx, InputStream convertedContent,
			DeliveryModel deliveryModel, FormatConverter hfc) throws Exception {
		return deliverViaServlet(convertedContent, hfc.getMimeType(),
				getFileName(deliveryModel), hfc.getFileExtension());
	}

	private String getFileName(DeliveryModel deliveryModel) {
		return deliveryModel.getSuggestedFileName().replaceAll("[:/]", "_");
	}

	protected String deliverViaServlet(InputStream stream, String mimeType,
			String suggestedFileName, String suffix) throws Exception {
		suggestedFileName = CommonUtils.trimToWsChars(suggestedFileName, 200,
				"... ");
		File file = File.createTempFile(suggestedFileName, "." + suffix);
		file.deleteOnExit();
		ResourceUtilities.writeStreamToStream(stream,
				new FileOutputStream(file));
		DownloadItem item = new DownloadServlet.DownloadItem(mimeType,
				suggestedFileName + "." + suffix, file.getPath());
		return DownloadServlet.add(item);
	}
}
