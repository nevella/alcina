package cc.alcina.framework.entity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;

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

		private boolean uppercaseTags;

		private Charset charset;

		private boolean decompress;

		private boolean base64;

		public byte[] asBytes() {
			try {
				InputStream stream = resource.getStream();
				int bufLength = stream.available() <= 1024 ? 1024 * 64
						: stream.available();
				ByteArrayOutputStream baos = new Streams.DisposableByteArrayOutputStream(
						bufLength);
				Io.Streams.copy(stream, baos);
				byte[] bytes = baos.toByteArray();
				if (base64) {
					bytes = Base64.getDecoder()
							.decode(new String(bytes, StandardCharsets.UTF_8));
				}
				return bytes;
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public Document asDocument() {
			return loadDocument();
		}

		public DomDocument asDomDocument() {
			return new DomDocument(asDocument());
		}

		public InputStream asInputStream() {
			try {
				return resource.getStream();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public List<String> asList() {
			return Arrays.asList(asString().split("\n"));
		}

		public StringMap asMap(MapType type) {
			String contents = asString();
			switch (type) {
			case EXISTENCE:
				return StringMap.fromStringList(contents);
			case KEYLINE_VALUELINE:
				return StringMap.fromKvStringList(contents, true);
			case PROPERTY:
				return StringMap.fromPropertyString(contents);
			default:
				throw new UnsupportedOperationException();
			}
		}

		public <T> T asObject() throws IOException {
			byte[] bytes = asBytes();
			try (ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(bytes))) {
				try {
					return (T) in.readObject();
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		}

		public String asString() {
			decompress = true;
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

		public ReadOp withBase64(boolean base64) {
			this.base64 = base64;
			return this;
		}

		public ReadOp withCharset(Charset charset) {
			this.charsetName = charset.name();
			return this;
		}

		public ReadOp withCharset(String charsetName) {
			Preconditions.checkNotNull(charsetName);
			this.charsetName = charsetName;
			return this;
		}

		public ReadOp withDecompress(boolean decompress) {
			this.decompress = decompress;
			return this;
		}

		public ReadOp withUppercaseTags(boolean uppercaseTags) {
			this.uppercaseTags = uppercaseTags;
			return this;
		}

		public WriteOp.Resource write() {
			try {
				return Io.write().inputStream(resource.getStream());
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		private DOMParser createDOMParser() {
			DOMParser parser = new DOMParser(new HTMLConfiguration());
			try {
				parser.setFeature(
						"http://cyberneko.org/html/features/scanner/fix-mswindows-refs",
						true);
				parser.setFeature(
						"http://cyberneko.org/html/features/scanner/ignore-specified-charset",
						true);
				if (!uppercaseTags) {
					parser.setProperty(
							"http://cyberneko.org/html/properties/names/elems",
							"lower");
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			return parser;
		}

		private Document loadDocument() {
			try {
				InputStream stream = resource.getStream();
				InputSource isrc = null;
				if (charset == null) {
					isrc = new InputSource(stream);
				} else {
					isrc = new InputSource(
							new InputStreamReader(stream, charset));
				}
				DOMParser parser = createDOMParser();
				parser.parse(isrc);
				return (Document) parser.getDocument();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public enum MapType {
			EXISTENCE, KEYLINE_VALUELINE, PROPERTY
		}

		public class Resource {
			private String path;

			private File file;

			private String classpathResource;

			private Class classpathRelative;

			private byte[] bytes;

			private InputStream stream;

			private String url;

			public ReadOp bytes(byte[] bytes) {
				this.stream = new ByteArrayInputStream(bytes);
				return ReadOp.this;
			}

			public ReadOp file(File file) {
				this.file = file;
				return ReadOp.this;
			}

			public ReadOp inputStream(InputStream stream) {
				this.stream = stream;
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
				if (classpathRelative == null) {
					classpathRelative = StackWalker
							.getInstance(
									StackWalker.Option.RETAIN_CLASS_REFERENCE)
							.getCallerClass();
				}
				return ReadOp.this;
			}

			public ReadOp string(String string) {
				bytes = string.getBytes(StandardCharsets.UTF_8);
				return ReadOp.this;
			}

			public ReadOp url(String url) {
				this.url = url;
				return ReadOp.this;
			}

			private void ensureFile() {
				if (path != null) {
					file = new File(path);
				}
			}

			private InputStream getStream() throws IOException {
				ensureFile();
				if (file != null) {
					stream = new FileInputStream(file);
					if (file.getName().endsWith(".gz") && decompress) {
						stream = new GZIPInputStream(
								new BufferedInputStream(stream));
					}
				} else if (classpathRelative != null) {
					stream = classpathRelative
							.getResourceAsStream(classpathResource);
					if (stream == null) {
						stream = Thread.currentThread().getContextClassLoader()
								.getResourceAsStream(classpathResource);
					}
				} else if (url != null) {
					if (url.startsWith("http")) {
						stream = new ByteArrayInputStream(
								new SimpleHttp(url).asBytes());
					} else {
						stream = new URL(url).openStream();
					}
				} else if (bytes != null) {
					stream = new ByteArrayInputStream(bytes);
				}
				return Io.Streams.maybeWrapInBufferedStream(stream);
			}
		}
	}

	public static class Streams {
		public static void copy(InputStream is, OutputStream os)
				throws IOException {
			copy(is, os, false);
		}

		public static void copy(InputStream in, OutputStream os,
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

		public static InputStream
				maybeWrapInBufferedStream(InputStream stream) {
			if (stream instanceof FileInputStream) {
				return new BufferedInputStream(stream, 64 * 1024);
			} else {
				return stream;
			}
		}

		/*
		 * Doesn't do a defensive copy of the internal byte array when calling
		 * toByteArray (so toByteArray can only be used once, as the last
		 * operation on the instance)
		 */
		static class DisposableByteArrayOutputStream
				extends ByteArrayOutputStream {
			public DisposableByteArrayOutputStream(int size) {
				super(size);
			}

			@Override
			public synchronized byte[] toByteArray() {
				if (count == buf.length) {
					byte[] ref = buf;
					buf = null;
					return ref;
				} else {
					return super.toByteArray();
				}
			}
		}
	}

	public static class WriteOp {
		private Contents contents = new Contents();

		private Resource resource = new Resource();

		public boolean noUpdate;

		public void toFile(File file) {
		}

		public void write() {
			try {
				if (noUpdate) {
					resource.ensureFile();
					if (resource.file != null && resource.file.exists()) {
						byte[] existingBytes = Io.read().file(resource.file)
								.asBytes();
						byte[] outputBytes = Io.read()
								.inputStream(contents.getStream()).asBytes();
						if (Arrays.equals(existingBytes, outputBytes)) {
							return;
						}
						contents = new Contents();
						contents.inputStream = new ByteArrayInputStream(
								outputBytes);
					}
				}
				Io.Streams.copy(contents.getStream(), resource.getStream());
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public class Contents {
			private String string;

			private byte[] bytes;

			private InputStream inputStream;

			public Resource bytes(byte[] bytes) {
				this.bytes = bytes;
				return WriteOp.this.resource;
			}

			public Resource inputStream(InputStream inputStream) {
				this.inputStream = inputStream;
				return WriteOp.this.resource;
			}

			public Resource object(Object object) {
				try {
					ByteArrayOutputStream baos = new Streams.DisposableByteArrayOutputStream(
							1024);
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(object);
					oos.close();
					this.inputStream = new ByteArrayInputStream(
							baos.toByteArray());
					return WriteOp.this.resource;
				} catch (Exception e) {
					throw WrappedRuntimeException.wrap(e);
				}
			}

			public Resource string(String string) {
				this.string = string;
				return WriteOp.this.resource;
			}

			private InputStream getStream() {
				if (inputStream == null) {
					inputStream = new ByteArrayInputStream(bytes != null ? bytes
							: string.getBytes(StandardCharsets.UTF_8));
				}
				return inputStream;
			}
		}

		public class Resource {
			private String path;

			private File file;

			private OutputStream stream;

			public String toBase64String() {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				this.stream = baos;
				WriteOp.this.write();
				String asB64 = Base64.getEncoder()
						.encodeToString(baos.toByteArray());
				return asB64;
			}

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

			public Resource withNoUpdate(boolean noUpdate) {
				WriteOp.this.noUpdate = noUpdate;
				return this;
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
