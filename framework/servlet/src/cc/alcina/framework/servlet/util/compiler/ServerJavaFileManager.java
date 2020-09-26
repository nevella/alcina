/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.alcina.framework.servlet.util.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import sun.misc.Unsafe;

/*
 * from net/openhft/compiler - reworking to VFS
 */
class ServerJavaFileManager implements JavaFileManager {
	private final static Unsafe unsafe;

	private static final long OVERRIDE_OFFSET;

	public static String classPath;
	static {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			unsafe = (Unsafe) theUnsafe.get(null);
			Field f = AccessibleObject.class.getDeclaredField("override");
			OVERRIDE_OFFSET = unsafe.objectFieldOffset(f);
		} catch (Exception ex) {
			throw new AssertionError(ex);
		}
	}

	static StandardJavaFileManager fileManager;

	// synchronizing due to ConcurrentModificationException
	private final Map<String, ByteArrayOutputStream> buffers = Collections
			.synchronizedMap(new LinkedHashMap<>());

	ServerJavaFileManager(StandardJavaFileManager fileManager) {
		ServerJavaFileManager.fileManager = fileManager;
	}

	public void clearBuffers() {
		buffers.clear();
	}

	@Override
	public void close() throws IOException {
		fileManager.close();
	}

	@Override
	public void flush() {
		// Do nothing
	}

	public Map<String, byte[]> getAllBuffers() {
		synchronized (buffers) {
			Map<String, byte[]> ret = new LinkedHashMap<>(buffers.size() * 2);
			for (Map.Entry<String, ByteArrayOutputStream> entry : buffers
					.entrySet()) {
				ret.put(entry.getKey(), entry.getValue().toByteArray());
			}
			return ret;
		}
	}

	@Override
	public ClassLoader getClassLoader(Location location) {
		return fileManager.getClassLoader(location);
	}

	@Override
	public FileObject getFileForInput(Location location, String packageName,
			String relativeName) throws IOException {
		return fileManager.getFileForInput(location, packageName, relativeName);
	}

	@Override
	public FileObject getFileForOutput(Location location, String packageName,
			String relativeName, FileObject sibling) throws IOException {
		return fileManager.getFileForOutput(location, packageName, relativeName,
				sibling);
	}

	@Override
	public JavaFileObject getJavaFileForInput(Location location,
			String className, Kind kind) throws IOException {
		if (location == StandardLocation.CLASS_OUTPUT) {
			boolean success = false;
			final byte[] bytes;
			synchronized (buffers) {
				success = buffers.containsKey(className) && kind == Kind.CLASS;
				bytes = buffers.get(className).toByteArray();
			}
			if (success) {
				return new SimpleJavaFileObject(URI.create(className), kind) {
					@Override
					public InputStream openInputStream() {
						return new ByteArrayInputStream(bytes);
					}
				};
			}
		}
		return fileManager.getJavaFileForInput(location, className, kind);
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location,
			final String className, Kind kind, FileObject sibling) {
		return new SimpleJavaFileObject(URI.create(className), kind) {
			@Override
			public OutputStream openOutputStream() {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				buffers.put(className, baos);
				return baos;
			}
		};
	}

	@Override
	public boolean handleOption(String current, Iterator<String> remaining) {
		return fileManager.handleOption(current, remaining);
	}

	@Override
	public boolean hasLocation(Location location) {
		return fileManager.hasLocation(location);
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		return fileManager.inferBinaryName(location, file);
	}

	public String inferModuleName(final Location location) {
		return invokeNamedMethodIfAvailable(location, "inferModuleName");
	}

	@Override
	public boolean isSameFile(FileObject a, FileObject b) {
		return fileManager.isSameFile(a, b);
	}

	@Override
	public int isSupportedOption(String option) {
		return fileManager.isSupportedOption(option);
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName,
			Set<Kind> kinds, boolean recurse) throws IOException {
		return fileManager.list(location, packageName, kinds, recurse);
	}

	public Iterable<Set<Location>>
			listLocationsForModules(final Location location) {
		return invokeNamedMethodIfAvailable(location,
				"listLocationsForModules");
	}

	@SuppressWarnings("unchecked")
	private <T> T invokeNamedMethodIfAvailable(final Location location,
			final String name) {
		final Method[] methods = fileManager.getClass().getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().equals(name)
					&& method.getParameterTypes().length == 1
					&& method.getParameterTypes()[0] == Location.class) {
				try {
					unsafe.putBoolean(method, OVERRIDE_OFFSET, true);
					return (T) method.invoke(fileManager, location);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new UnsupportedOperationException(
							"Unable to invoke method " + name);
				}
			}
		}
		throw new UnsupportedOperationException(
				"Unable to find method " + name);
	}
}