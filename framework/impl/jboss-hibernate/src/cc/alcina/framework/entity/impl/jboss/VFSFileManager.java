package cc.alcina.framework.entity.impl.jboss;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

public class VFSFileManager extends com.sun.tools.javac.util.BaseFileManager
		implements StandardJavaFileManager {
	public VFSFileManager() {
		super(StandardCharsets.UTF_8);
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public ClassLoader getClassLoader(Location location) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileObject getFileForInput(Location location, String packageName,
			String relativeName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileObject getFileForOutput(Location location, String packageName,
			String relativeName, FileObject sibling) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JavaFileObject getJavaFileForInput(Location location,
			String className, Kind kind) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location,
			String className, Kind kind, FileObject sibling)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<? extends JavaFileObject>
			getJavaFileObjects(File... files) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<? extends JavaFileObject>
			getJavaFileObjects(String... names) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<? extends JavaFileObject>
			getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<? extends JavaFileObject>
			getJavaFileObjectsFromStrings(Iterable<String> names) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<? extends File> getLocation(Location location) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasLocation(Location location) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDefaultBootClassPath() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSameFile(FileObject a, FileObject b) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName,
			Set<Kind> kinds, boolean recurse) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLocation(Location location, Iterable<? extends File> path)
			throws IOException {
		// TODO Auto-generated method stub
	}
}