package cc.alcina.framework.entity.util;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.ProcessCounter;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.SEUtilities;

@Registration(ProcessLogFolder.class)
public abstract class ProcessLogFolder {
	transient Logger logger = LoggerFactory.getLogger(getClass());

	public abstract String getFolder();

	public void reap() {
		File file = DataFolderProvider.get().getChildFile(getFolder());
		file.mkdirs();
		List<File> files = SEUtilities.listFilesRecursive(file.getPath(), null,
				true);
		ProcessCounter counter = new ProcessCounter();
		files.stream().peek(f -> counter.visited())
				.filter(f -> TimeConstants.within(f.lastModified(), maxAge()))
				.forEach(f -> {
					f.delete();
					counter.modified();
				});
		logger.info("Reaped {} - {}", getFolder(), counter);
	}

	protected long maxAge() {
		return TimeConstants.ONE_DAY_MS;
	}
}