package cc.alcina.framework.servlet.task;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskReplaceSymlinksWithTargets
		extends ServerTask<TaskReplaceSymlinksWithTargets> {
	private String root;

	public String getRoot() {
		return this.root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	@Override
	protected void performAction0(TaskReplaceSymlinksWithTargets task)
			throws Exception {
		Files.walkFileTree(Paths.get(root), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				if (Files.isSymbolicLink(file)) {
					Path target = Files.readSymbolicLink(file);
					logger.info("{} => {}", target, file);
					Files.copy(target, file,
							StandardCopyOption.REPLACE_EXISTING);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
