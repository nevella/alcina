package cc.alcina.framework.servlet.task;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.servlet.schedule.PerformerTask;

@Bean(PropertySource.FIELDS)
public class TaskIncrementalDelete extends PerformerTask {
	public String path;

	public boolean logFiles = true;

	transient SystemoutCounter counter = new SystemoutCounter(100, 100);

	@Override
	public void run() throws Exception {
		FileVisitor<? super Path> visitor = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				file.toFile().delete();
				if (logFiles) {
					logger.info("deleted {}", file);
				}
				counter.tick();
				return FileVisitResult.CONTINUE;
			}
		};
		Files.walkFileTree(Path.of(path), visitor);
	}
}
