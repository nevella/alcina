package cc.alcina.framework.entity.util.fs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class WatchablePath implements Path {
	private Path delegate;

	public WatchablePath(Path delegate) {
		this.delegate = delegate;
	}

	public int compareTo(Path other) {
		return this.delegate.compareTo(other);
	}

	public boolean endsWith(Path other) {
		return this.delegate.endsWith(other);
	}

	public boolean endsWith(String other) {
		return this.delegate.endsWith(other);
	}

	public boolean equals(Object other) {
		return this.delegate.equals(other);
	}

	public void forEach(Consumer<? super Path> action) {
		this.delegate.forEach(action);
	}

	public Path getFileName() {
		return this.delegate.getFileName();
	}

	public FileSystem getFileSystem() {
		return this.delegate.getFileSystem();
	}

	public Path getName(int index) {
		return this.delegate.getName(index);
	}

	public int getNameCount() {
		return this.delegate.getNameCount();
	}

	public Path getParent() {
		return this.delegate.getParent();
	}

	public Path getRoot() {
		return this.delegate.getRoot();
	}

	public int hashCode() {
		return this.delegate.hashCode();
	}

	public boolean isAbsolute() {
		return this.delegate.isAbsolute();
	}

	public Iterator<Path> iterator() {
		return this.delegate.iterator();
	}

	public Path normalize() {
		return this.delegate.normalize();
	}

	public WatchKey register(WatchService watcher, Kind<?>... events)
			throws IOException {
		if (watcher instanceof AbstractNonSunWatchService) {
			return ((AbstractNonSunWatchService) watcher).register(this,
					events);
		} else {
			return this.delegate.register(watcher, events);
		}
	}

	public WatchKey register(WatchService watcher, Kind<?>[] events,
			Modifier... modifiers) throws IOException {
		if (watcher instanceof AbstractNonSunWatchService) {
			return ((AbstractNonSunWatchService) watcher).register(this, events,
					modifiers);
		} else {
			return this.delegate.register(watcher, events, modifiers);
		}
	}

	public Path relativize(Path other) {
		return this.delegate.relativize(other);
	}

	public Path resolve(Path other) {
		return this.delegate.resolve(other);
	}

	public Path resolve(String other) {
		return this.delegate.resolve(other);
	}

	public Path resolveSibling(Path other) {
		return this.delegate.resolveSibling(other);
	}

	public Path resolveSibling(String other) {
		return this.delegate.resolveSibling(other);
	}

	public Spliterator<Path> spliterator() {
		return this.delegate.spliterator();
	}

	public boolean startsWith(Path other) {
		return this.delegate.startsWith(other);
	}

	public boolean startsWith(String other) {
		return this.delegate.startsWith(other);
	}

	public Path subpath(int beginIndex, int endIndex) {
		return this.delegate.subpath(beginIndex, endIndex);
	}

	public Path toAbsolutePath() {
		return this.delegate.toAbsolutePath();
	}

	public File toFile() {
		return this.delegate.toFile();
	}

	public Path toRealPath(LinkOption... options) throws IOException {
		return this.delegate.toRealPath(options);
	}

	public String toString() {
		return this.delegate.toString();
	}

	public URI toUri() {
		return this.delegate.toUri();
	}
}