package cc.alcina.framework.entity.util.decorator;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import cc.alcina.framework.common.client.util.ThrowingConsumer;

public class FileVisitorFileConsumer extends SimpleFileVisitor<Path> {
	private ThrowingConsumer<Path> consumer;

	public FileVisitorFileConsumer(ThrowingConsumer<Path> consumer) {
		this.consumer = consumer;
	}

	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
			throws IOException {
		try {
			consumer.accept(path);
		} catch (Exception e) {
			throw new IOException(e);
		}
		return FileVisitResult.CONTINUE;
	}
}
