package cc.alcina.framework.servlet.publication.delivery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.process.ContextObservers;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.ContentDeliveryType;
import cc.alcina.framework.common.client.publication.ContentDeliveryType.ContentDeliveryType_LOCAL_FILESYSTEM;
import cc.alcina.framework.common.client.publication.DeliveryModel;
import cc.alcina.framework.common.client.publication.HasLocalDelivery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.entity.util.ZipUtil;
import cc.alcina.framework.servlet.publication.FormatConverter;
import cc.alcina.framework.servlet.publication.PublicationContext;

/**
 * 
 */
@Registration({ ContentDeliveryType.class,
		ContentDeliveryType_LOCAL_FILESYSTEM.class })
public class ContentDeliveryLocalFilesystem implements ContentDelivery {
	Logger logger = LoggerFactory.getLogger(getClass());

	public static class FileGenerated implements ContextObservers.Observable {
		public String path;

		public FileGenerated(File file) {
			path = file.getPath();
		}

		public FileGenerated(String path) {
			this(new File(path));
		}

		File file() {
			return new File(path);
		}
	}

	public static class GeneratedObserver
			implements ContextObservers.Observer<FileGenerated> {
		public GeneratedObserver() {
		}

		public List<FileGenerated> observed = new ArrayList<>();

		@Override
		public void topicPublished(FileGenerated message) {
			observed.add(message);
		}

		public void zipTo(String path) {
			String longestCommon = TextUtils.longestCommon(
					observed.stream().map(fg -> fg.file().getName())
							.collect(Collectors.toList()));
			longestCommon = Ax.blankTo(longestCommon, "archive");
			String zipPath = Ax.format("%s/%s.zip", path, longestCommon);
			ZipUtil.createZip(new File(zipPath),
					observed.stream().map(FileGenerated::file).toList());
			LoggerFactory.getLogger(getClass()).info(
					"Archived {} generated files to {}", observed.size(),
					zipPath);
		}
	}

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
		new FileGenerated(file).publish();
		logger.info("  --  ", file);
		return file.getPath();
	}
}
