package cc.alcina.framework.entity.util;

import java.io.File;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;

/**
 * For capturing the results of complex, asynchronous processes - particularly
 * debugging support
 *
 * Kinda sorta the treelogger, it provides the support for detailed
 * logging/reproduction _if_ triggered (generally by an exception or a user
 * action)
 *
 */
public abstract class ProcessLogger<T> {
	public static final transient String FOLDER = "process-logger";

	private static transient Logger logger = LoggerFactory
			.getLogger(ProcessLogger.class);

	public void inject() {
		LooseContext.set(getClass().getName(), this);
	}

	public String persist() {
		try {
			String subfolder = SEUtilities.getNestedSimpleName(getClass());
			String fileName = Ax.format("log-%s.json",
					Ax.timestampYmd(new Date()));
			String path = Ax.format("%s/%s/%s", FOLDER, subfolder, fileName);
			File file = DataFolderProvider.get().getChildFile(path);
			file.getParentFile().mkdirs();
			JacksonUtils.serializeToFile(this, file);
			logger.info("ProcessLogger.logged: {}", path);
			return file.getAbsolutePath();
		} catch (Exception e) {
			logger.info("DEVEX-0: {}", CommonUtils.toSimpleExceptionMessage(e));
			e.printStackTrace();
			return null;
		}
	}

	public static class ProcessLogFolder_ProcessLogger
			extends ProcessLogFolder {
		@Override
		public String getFolder() {
			return FOLDER;
		}
	}
}
