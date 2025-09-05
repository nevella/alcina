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
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FileLogger;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Io.WriteOp.Contents;

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
		private String string;

		private byte[] bytes;

		public void toFile(byte[] bytes) {
			this.bytes = bytes;
			log();
		}

		public void toFile(String string) {
			this.string = string;
			log();
		}

		void log() {
			writeFile("log.txt");
			writeFile("log.html");
			writeFile("log.xml");
		}

		private void writeFile(String fileName) {
			try {
				new File("/tmp/log").mkdirs();
				String path = "/tmp/log/" + fileName;
				Contents contents = write();
				WriteOp.Resource resource = null;
				if (string != null) {
					resource = contents.string(string);
				} else {
					resource = contents.bytes(bytes);
				}
				resource.toPath(path);
				Ax.out("Logged to: %s ", path);
				Ax.out("");
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

		private boolean decompressIfDotGz;

		private InputStream stream;

		InputStream ensureStream() {
			if (stream == null) {
				try {
					stream = resource.getStream();
				} catch (Exception e) {
					Ax.out("Error accessing resource :: %s", resource);
					throw WrappedRuntimeException.wrap(e);
				}
			}
			return stream;
		}

		private Class<?> kryoType;

		public byte[] asBytes() {
			try {
				InputStream stream = ensureStream();
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
				Ax.out("Error accessing resource :: %s", resource);
				throw WrappedRuntimeException.wrap(e);
			}
		}

		/**
		 * Return a domdocument generated from parsing the input (by default) as
		 * an HTML (not XML) document
		 */
		public DomDocument asDomDocument() {
			return DomDocument.from(asHtmlDocument());
		}

		/*
		 * Will proccess as an HTML doc *unless* the doc begins with <?xml
		 */
		public Document asHtmlDocument() {
			resource.replayable = true;
			String markup = asString();
			try {
				resource.stream.reset();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
			if (markup.startsWith("<?xml")) {
				return asXmlDocument();
			} else {
				return loadDocument();
			}
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

		public <T> T asObject() {
			try {
				if (kryoType != null) {
					return (T) KryoUtils.deserializeFromBase64(asString(),
							kryoType);
				} else {
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
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public <T> T asReflectiveSerializedObject() {
			String s = asString();
			return ReflectiveSerializer.deserialize(s);
		}

		public String asString() {
			decompressIfDotGz = true;
			byte[] bytes = asBytes();
			try {
				return new String(bytes, charsetName);
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public Document asXmlDocument() {
			try {
				return XmlUtils.loadDocument(resource.getStream());
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		/**
		 * Return a domdocument generated from parsing the input as an XML (not
		 * HTML) document
		 */
		public DomDocument asXmlDomDocument() {
			return DomDocument.from(asXmlDocument());
		}

		public boolean exists() {
			try (InputStream stream = resource.getStream()) {
				return stream != null;
			} catch (IOException e) {
				return false;
			}
		}

		private Document loadDocument() {
			try {
				InputStream stream = resource.getStream();
				InputSource isrc = null;
				if (charset == null) {
					isrc = new InputSource(new InputStreamReader(stream,
							StandardCharsets.UTF_8));
				} else {
					isrc = new InputSource(
							new InputStreamReader(stream, charset));
				}
				DOMParser parser = DomParserUtils
						.createDOMParser(!uppercaseTags);
				parser.parse(isrc);
				return (Document) parser.getDocument();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
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

		public ReadOp withKryoType(Class<?> kryoType) {
			this.kryoType = kryoType;
			return this;
		}

		public ReadOp withUppercaseTags(boolean uppercaseTags) {
			this.uppercaseTags = uppercaseTags;
			return this;
		}

		public WriteOp.Resource write() {
			try {
				return Io.write().fromStream(resource.getStream());
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

			boolean replayable;

			public ReadOp bytes(byte[] bytes) {
				this.stream = new ByteArrayInputStream(bytes);
				return ReadOp.this;
			}

			private void ensureFile() {
				if (path != null) {
					file = new File(path);
				}
			}

			public ReadOp file(File file) {
				this.file = file;
				return ReadOp.this;
			}

			public ReadOp fromStream(InputStream stream) {
				this.stream = stream;
				return ReadOp.this;
			}

			private InputStream getStream() throws IOException {
				ensureFile();
				boolean decompress = ReadOp.this.decompress;
				if (file != null) {
					stream = new FileInputStream(file);
					decompress |= decompressIfDotGz
							&& file.getName().endsWith(".gz");
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
				stream = Io.Streams.maybeWrapInBufferedStream(stream);
				if (decompress) {
					stream = new GZIPInputStream(stream);
				}
				if (replayable) {
					if (stream instanceof ByteArrayInputStream) {
					} else {
						int bufLength = stream.available() <= 1024 ? 1024 * 64
								: stream.available();
						ByteArrayOutputStream baos = new Streams.DisposableByteArrayOutputStream(
								bufLength);
						Io.Streams.copy(stream, baos);
						stream = new ByteArrayInputStream(baos.toByteArray());
					}
				}
				return stream;
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
					if (Configuration.useStackTraceCallingClass) {
						classpathRelative = Configuration
								.getStacktraceCallingClass();
					} else {
						classpathRelative = StackWalker.getInstance(
								StackWalker.Option.RETAIN_CLASS_REFERENCE)
								.getCallerClass();
					}
				}
				return ReadOp.this;
			}

			public ReadOp string(String string) {
				bytes = string.getBytes(StandardCharsets.UTF_8);
				return ReadOp.this;
			}

			@Override
			public String toString() {
				return FormatBuilder.keyValues("path", path, "url", url,
						"classpathRelative", classpathRelative,
						"classpathResource", classpathResource);
			}

			public ReadOp url(String url) {
				this.url = url;
				return ReadOp.this;
			}

			public ReadOp base64String(String base64String) {
				bytes = Base64.getDecoder().decode(base64String);
				return ReadOp.this;
			}
		}

		/**
		 * 
		 * @return the input as a data url of type application/octet-stream
		 */
		public String asDataUrl() {
			return asDataUrl("application/octet-stream");
		}

		public String asDataUrl(String mimeType) {
			try {
				String url = Ax.format("data:%s;base64,%s", mimeType,
						asBase64String());
				return url;
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public String asBase64String() {
			return Base64.getEncoder().encodeToString(asBytes());
		}

		public Optional<String> asStringOptional() {
			if (ensureStream() == null) {
				return Optional.empty();
			} else {
				return Optional.of(asString());
			}
		}

		public DomDocument asXmlOrHtmlDomDocument() {
			resource.replayable = true;
			String markup = asString();
			try {
				resource.stream.reset();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
			byte[] asBytes = asBytes();
			try {
				LooseContext
						.pushWithTrue(XmlUtils.CONTEXT_MUTE_XML_SAX_EXCEPTIONS);
				return asXmlDomDocument();
			} catch (Exception e) {
				return asDomDocument();
			} finally {
				LooseContext.pop();
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

		private boolean noUpdateIdentical;

		private boolean compress;

		public boolean ensureParents;

		public void write() {
			try {
				if (noUpdateIdentical) {
					resource.ensureFile();
					if (resource.file != null && resource.file.exists()) {
						byte[] existingBytes = Io.read().file(resource.file)
								.asBytes();
						byte[] outputBytes = Io.read()
								.fromStream(contents.getStream()).asBytes();
						if (Arrays.equals(existingBytes, outputBytes)) {
							return;
						}
						contents = new Contents();
						contents.inputStream = new ByteArrayInputStream(
								outputBytes);
					}
				}
				if (ensureParents) {
					resource.ensureFile();
					resource.file.getParentFile().mkdirs();
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

			private boolean reflectiveSerialized;

			private boolean kryo;

			public Contents
					asReflectiveSerialized(boolean reflectiveSerialized) {
				this.reflectiveSerialized = reflectiveSerialized;
				return this;
			}

			public Contents asKryo(boolean kryo) {
				this.kryo = kryo;
				return this;
			}

			public Resource bytes(byte[] bytes) {
				this.bytes = bytes;
				return WriteOp.this.resource;
			}

			public Resource fromStream(InputStream inputStream) {
				this.inputStream = inputStream;
				return WriteOp.this.resource;
			}

			private InputStream getStream() {
				if (inputStream == null) {
					inputStream = new ByteArrayInputStream(bytes != null ? bytes
							: string.getBytes(StandardCharsets.UTF_8));
				}
				return inputStream;
			}

			public Resource object(Object object) {
				if (reflectiveSerialized) {
					this.string = ReflectiveSerializer.serialize(object);
					return WriteOp.this.resource;
				} else if (kryo) {
					this.string = KryoUtils.serializeToBase64(object);
					return WriteOp.this.resource;
				} else {
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
			}

			public Resource string(String string) {
				this.string = string;
				return WriteOp.this.resource;
			}

			public Resource stringList(List<String> list) {
				return string(CommonUtils.join(list, "\n"));
			}
		}

		public class Resource {
			private String path;

			private File file;

			private OutputStream stream;

			private void ensureFile() {
				if (path != null && file == null) {
					file = new File(path);
				}
			}

			private OutputStream getStream() throws IOException {
				ensureFile();
				if (stream == null) {
					stream = new BufferedOutputStream(
							new FileOutputStream(file));
				}
				if (compress) {
					stream = new GZIPOutputStream(stream);
				}
				return stream;
			}

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

			/**
			 * Careful! If writing to an http stream, you'll need to add
			 * Content-Encoding: gzp
			 */
			public Resource withCompress(boolean compress) {
				WriteOp.this.compress = compress;
				return this;
			}

			public Resource withEnsureParents(boolean ensureParents) {
				WriteOp.this.ensureParents = ensureParents;
				return this;
			}

			/**
			 * Do not update the file (i.e. the timestamp, via a rewrite) if its
			 * content is identical to the content being written
			 */
			public Resource withNoUpdateIdentical(boolean noUpdateIdentical) {
				WriteOp.this.noUpdateIdentical = noUpdateIdentical;
				return this;
			}
		}
	}

	@Registration.Singleton(FileLogger.class)
	public static class FileLoggerImpl implements FileLogger {
		@Override
		public void logImpl(String text) {
			Io.log().toFile(text);
		}
	}
}
