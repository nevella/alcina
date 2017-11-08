package cc.alcina.framework.servlet.publication.delivery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_DOWNLOAD_ATTACHMENT;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_LOCAL_FILESYSTEM;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;
import cc.alcina.framework.servlet.servlet.DownloadServlet;
import cc.alcina.framework.servlet.servlet.DownloadServlet.DownloadItem;

/**
 * 
 * @author nreddel@barnet.com.au
 *
 */
@RegistryLocation(registryPoint = ContentDeliveryType.class, targetClass = ContentDeliveryType_LOCAL_FILESYSTEM.class)
public class ContentDeliveryLocalFilesystem implements ContentDelivery {
    public String deliver(PublicationContext ctx, InputStream convertedContent,
            DeliveryModel deliveryModel, FormatConverter hfc) throws Exception {
        String suggestedFileName = deliveryModel.getSuggestedFileName();
        File file = new File(suggestedFileName);
        ResourceUtilities.writeStreamToStream(convertedContent,
                new FileOutputStream(file));
        return suggestedFileName;
    }
}
