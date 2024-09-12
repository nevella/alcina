package cc.alcina.framework.servlet.publication.delivery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_LOCAL_FILESYSTEM;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.HasLocalDelivery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

/**
 * 
 */
@Registration({ ContentDeliveryType.class,
		ContentDeliveryType_LOCAL_FILESYSTEM.class })
public class ContentDeliveryLocalFilesystem implements ContentDelivery {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public String deliver(PublicationContext ctx, InputStream convertedContent,
			DeliveryModel deliveryModel, FormatConverter hfc) throws Exception {
		String localDeliveryFolder = "/tmp";
		ContentDefinition contentDefinition = PublicationContext
				.get().contentDefinition;
		if (contentDefinition instanceof HasLocalDelivery) {
			HasLocalDelivery hld = (HasLocalDelivery) contentDefinition;
			localDeliveryFolder = hld.provideLocalDeliveryFolder();
		}
		String suggestedFileName = deliveryModel.getSuggestedFileName();
		File folder = new File(localDeliveryFolder);
		folder.mkdirs();
		if (!PermissionsManager.hasAdminAccessLevel()
				|| !Configuration.is("permitAbsoluteSuggestedPath")) {
			suggestedFileName = suggestedFileName.replace("/", "_")
					.replace("\\", "_");
		}
		if (Configuration.is("noSpaces")) {
			suggestedFileName = suggestedFileName.replace(" ", "_");
		}
		File file = suggestedFileName.startsWith("/")
				? new File(suggestedFileName)
				: FileUtils.child(folder, suggestedFileName);
		Io.Streams.copy(convertedContent, new FileOutputStream(file));
		logger.info("Wrote publication to local path: {}", file);
		if (Ax.isTest()) {
			File devPubFolder = new File("/tmp/pub");
			devPubFolder.mkdirs();
			File devOut = FileUtils.child(devPubFolder,
					file.getName().replaceFirst("(.+)\\.(.+)", "last.$2"));
			Io.read().file(file).write().toFile(devOut);
			logger.info("Wrote publication to local path: {}", devOut);
		}
		logger.info("  --  ", file);
		return file.getPath();
	}
}
