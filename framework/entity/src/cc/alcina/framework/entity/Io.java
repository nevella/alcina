package cc.alcina.framework.entity;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities.DisposableByteArrayOutputStream;

public class Io {
	public static LogOp log() {
		return new LogOp();
	}

	public static ReadOp.Resource read() {
		return new ReadOp().resource;
	}

	public static WriteOp.Contents write() {
		return new WriteOp().contents;
	}

	private static void writeStreamToStream(InputStream is, OutputStream os)
			throws IOException {
		writeStreamToStream(is, os, false);
	}

	private static void writeStreamToStream(InputStream in, OutputStream os,
			boolean keepOutputOpen) throws IOException {
		OutputStream bos = os instanceof ByteArrayOutputStream ? os
				: new BufferedOutputStream(os);
		int bufLength = in.available() <= 1024 ? 1024 * 64
				: Math.min(1024 * 1024, in.available());
		byte[] buffer = new byte[bufLength];
		int result;
		while ((result = in.read(buffer)) != -1) {
			bos.write(buffer, 0, result);
		}
		bos.flush();
		if (!keepOutputOpen) {
			bos.close();
		}
		in.close();
	}

	public static class LogOp {
		private String content;

		public void toFile(String content) {
			this.content = content;
			writeFile("log.txt");
			writeFile("log.html");
			writeFile("log.xml");
		}

		private void writeFile(String fileName) {
			try {
				new File("/tmp/log").mkdirs();
				String path = "/tmp/log/" + fileName;
				write().string(content).toPath(path);
				Ax.out("Logged to: %s ", path);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public static class ReadOp {
		private String charsetName = StandardCharsets.UTF_8.name();

		private Resource resource = new Resource();

		public byte[] asBytes() {
			try {
				InputStream stream = resource.getStream();
				int bufLength = stream.available() <= 1024 ? 1024 * 64
						: stream.available();
				ByteArrayOutputStream baos = new DisposableByteArrayOutputStream(
						bufLength);
				Io.writeStreamToStream(stream, baos);
				return baos.toByteArray();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public StringMap asMap(MapType type) {
			String contents = asString();
			switch (type) {
			case EXISTENCE:
				return StringMap.fromStringList(contents);
			case KEY_VALUE:
				return StringMap.fromKvStringList(contents, true);
			case PROPERTY:
				return StringMap.fromPropertyString(contents);
			default:
				throw new UnsupportedOperationException();
			}
		}

		public String asString() {
			byte[] bytes = asBytes();
			try {
				return new String(bytes, charsetName);
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public boolean exists() {
			try (InputStream stream = resource.getStream()) {
				return stream != null;
			} catch (IOException e) {
				return false;
			}
		}

		public ReadOp withCharset(String charsetName) {
			this.charsetName = charsetName;
			return this;
		}

		public enum MapType {
			EXISTENCE, KEY_VALUE, PROPERTY
		}

		public class Resource {
			private String path;

			private File file;

			private String classpathResource;

			private Class classpathRelative;

			public ReadOp file(String path) {
				this.path = path;
				return ReadOp.this;
			}

			public ReadOp path(String path) {
				this.path = path;
				return ReadOp.this;
			}

			public Resource relativeTo(Class clazz) {
				this.classpathRelative = clazz;
				return this;
			}

			public ReadOp resource(String classpathResource) {
				this.classpathResource = classpathResource;
				if (classpathRelative != null) {
					classpathRelative = StackWalker
							.getInstance(
									StackWalker.Option.RETAIN_CLASS_REFERENCE)
							.getCallerClass();
				}
				return ReadOp.this;
			}

			private void ensureFile() {
				if (path != null) {
					file = new File(path);
				}
			}

			private InputStream getStream() throws IOException {
				ensureFile();
				InputStream stream = null;
				if (file != null) {
					stream = new FileInputStream(file);
				} else if (classpathRelative != null) {
					stream = classpathRelative
							.getResourceAsStream(classpathResource);
					if (stream == null) {
						stream = Thread.currentThread().getContextClassLoader()
								.getResourceAsStream(classpathResource);
					}
				}
				return stream;
			}
		}
	}

	public static class WriteOp {
		private Contents contents = new Contents();

		private Resource resource = new Resource();

		public void toFile(File file) {
		}

		public void write() {
			try {
				writeStreamToStream(contents.getStream(), resource.getStream());
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public class Contents {
			private String string;

			private byte[] bytes;

			public Resource bytes(byte[] bytes) {
				this.bytes = bytes;
				return WriteOp.this.resource;
			}

			public InputStream getStream() {
				return new ByteArrayInputStream(bytes != null ? bytes
						: string.getBytes(StandardCharsets.UTF_8));
			}

			public Resource string(String string) {
				this.string = string;
				return WriteOp.this.resource;
			}
		}

		public class Resource {
			private String path;

			private File file;

			private OutputStream stream;

			public void toFile(File file) {
				this.file = file;
				WriteOp.this.write();
			}

			public void toPath(String path) {
				this.path = path;
				WriteOp.this.write();
			}

			public void toStream(OutputStream stream) {
				this.stream = stream;
				WriteOp.this.write();
			}

			private void ensureFile() {
				if (path != null) {
					file = new File(path);
				}
			}

			private OutputStream getStream() throws IOException {
				ensureFile();
				return stream != null ? stream : new FileOutputStream(file);
			}
		}
	}
}
