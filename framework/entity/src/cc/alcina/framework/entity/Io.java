package cc.alcina.framework.entity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities.DisposableByteArrayOutputStream;

public class Io {
	public static LogOp log() {
		return new LogOp();
	}

	public static ReadOp read() {
		return new ReadOp();
	}

	private static InputStream getResourceAsStream(Class clazz, String path) {
		InputStream stream = clazz.getResourceAsStream(path);
		if (stream == null) {
			stream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(path);
		}
		return stream;
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
				Write.writeStringToFile(content, path);
				Ax.out("Logged to: %s ", path);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public static class ReadOp {
		public static String read(Class clazz, String path) {
			return readClassPathResourceAsString(clazz, path);
		}

		public static String read(File file) {
			try {
				return readFileToString(file);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public static byte[] readClassPathResourceAsByteArray(Class clazz,
				String path) {
			try {
				return readStreamToByteArray(getResourceAsStream(clazz, path));
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public static String readClassPathResourceAsString(Class clazz,
				String path) {
			try {
				return readStreamToString(getResourceAsStream(clazz, path));
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public static String readClassPathResourceAsStringPreferFile(
				Class clazz, String path, String filePath) {
			File file = new File(filePath);
			if (Configuration.is(ResourceUtilities.class, "useDevResources")
					&& file.exists()) {
				try {
					return ResourceUtilities.readFileToString(file);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			} else {
				return readClassPathResourceAsString(clazz, path);
			}
		}

		public static byte[] readFileToByteArray(File f) throws IOException {
			FileInputStream fis = new FileInputStream(f);
			return readStreamToByteArray(fis);
		}

		public static String readFileToString(File f) throws IOException {
			InputStream fis = new FileInputStream(f);
			return readStreamToString(fis);
		}

		public static String readFileToString(File f, String charsetName)
				throws IOException {
			FileInputStream fis = new FileInputStream(f);
			return readStreamToString(fis, charsetName);
		}

		public static String readFileToString(String fileName)
				throws IOException {
			return readFileToString(new File(fileName));
		}

		public static String readFileToStringGz(File f) {
			try {
				InputStream fis = new FileInputStream(f);
				if (f.getName().endsWith(".gz")) {
					fis = new GZIPInputStream(new BufferedInputStream(fis));
				}
				return readStreamToString(fis);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public static <T> T readObjectFromBase64(String string)
				throws IOException {
			byte[] bytes = Base64.getDecoder().decode(string.trim());
			try (ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(bytes))) {
				try {
					return (T) in.readObject();
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		}

		public static <T> T readObjectFromBase64Url(String string)
				throws IOException {
			byte[] bytes = Base64.getUrlDecoder().decode(string.trim());
			try (ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(bytes))) {
				try {
					return (T) in.readObject();
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		}

		public static String readRelativeResource(String path) {
			return readClassPathResourceAsString(StackWalker
					.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
					.getCallerClass(), path);
		}

		public static byte[] readRelativeResourceAsBytes(String string) {
			// TODO Auto-generated method stub
			return null;
		}

		public static byte[] readStreamToByteArray(InputStream is)
				throws IOException {
			int bufLength = is.available() <= 1024 ? 1024 * 64 : is.available();
			ByteArrayOutputStream baos = new DisposableByteArrayOutputStream(
					bufLength);
			Write.writeStreamToStream(is, baos);
			return baos.toByteArray();
		}

		public static String readStreamToString(InputStream is)
				throws IOException {
			return readStreamToString(is, null);
		}

		public static String readStreamToString(InputStream is,
				String charsetName) throws IOException {
			try {
				byte[] bytes = readStreamToByteArray(is);
				charsetName = charsetName == null ? "UTF-8" : charsetName;
				return new String(bytes, charsetName);
			} finally {
				is.close();
			}
		}

		public static byte[] readUrlAsByteArray(String strUrl)
				throws IOException {
			URL url = new URL(strUrl);
			InputStream is = null;
			is = url.openConnection().getInputStream();
			return readStreamToByteArray(is);
		}

		public static byte[] readUrlAsBytesWithPost(String strUrl,
				String postBody, StringMap headers) throws Exception {
			return new SimpleHttp(strUrl).withPostBody(postBody)
					.withHeaders(headers).asBytes();
		}

		public static byte[] readUrlAsBytesWithPut(String strUrl, String body,
				StringMap headers) throws Exception {
			return new SimpleHttp(strUrl).withPutMethod().withBody(body)
					.withHeaders(headers).asBytes();
		}

		public static String readUrlAsString(String strUrl) throws Exception {
			return readUrlAsString(strUrl, null);
		}

		public static String readUrlAsString(String strUrl, String charset)
				throws Exception {
			// don't use
			// cc.alcina.framework.entity.ResourceUtilities.readUrlAsString(String,
			// String, StringMap)
			// we a java UA inter alia
			URL url = new URL(strUrl);
			InputStream is = null;
			URLConnection openConnection = url.openConnection();
			is = openConnection.getInputStream();
			String input = readStreamToString(is, charset);
			return input;
		}

		public static String readUrlAsString(String strUrl, String charset,
				StringMap headers) throws Exception {
			InputStream in = null;
			HttpURLConnection connection = null;
			try {
				URL url = new URL(strUrl);
				connection = (HttpURLConnection) (url.openConnection());
				connection.setDoOutput(false);
				connection.setDoInput(true);
				connection.setUseCaches(false);
				connection.setRequestMethod("GET");
				for (Entry<String, String> e : headers.entrySet()) {
					connection.setRequestProperty(e.getKey(), e.getValue());
				}
				in = connection.getInputStream();
				String input = readStreamToString(in);
				return input;
			} catch (IOException ioe) {
				if (connection != null) {
					InputStream err = connection.getErrorStream();
					String input = err == null ? null : readStreamToString(err);
					throw new IOException(input, ioe);
				} else {
					throw ioe;
				}
			} finally {
				if (in != null) {
					in.close();
				}
				if (connection != null) {
					connection.disconnect();
				}
			}
		}

		public static String readUrlAsStringWithPost(String strUrl,
				String postBody, StringMap headers) throws Exception {
			byte[] bytes = readUrlAsBytesWithPost(strUrl, postBody, headers);
			return new String(bytes, StandardCharsets.UTF_8);
		}

		public static String readUrlAsStringWithPut(String strUrl, String body,
				StringMap headers) throws Exception {
			byte[] bytes = readUrlAsBytesWithPut(strUrl, body, headers);
			return new String(bytes, StandardCharsets.UTF_8);
		}

		private String path;

		private File file;

		private String charsetName = StandardCharsets.UTF_8.name();

		private String classpathResource;

		private Class classpathRelative;

		public String asString() {
			try {
				InputStream stream = getStream();
				return readStreamToString(stream, charsetName);
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		public String read(String path) {
			try {
				return readFileToString(path);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public ReadOp withClasspathResource(String classpathResource) {
			this.classpathResource = classpathResource;
			classpathRelative = StackWalker
					.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
					.getCallerClass();
			return this;
		}

		public ReadOp withPath(String path) {
			this.path = path;
			return this;
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

	public static class Write {
		public static void write(String content, File file) {
			try {
				writeStringToFile(content, file);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public static void write(String content, String path) {
			try {
				writeStringToFile(content, path);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public static void writeBytesToFile(byte[] bytes, File dataFile)
				throws IOException {
			writeStreamToStream(new ByteArrayInputStream(bytes),
					new FileOutputStream(dataFile));
		}

		public static String writeObjectAsBase64(Object object)
				throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			oos.close();
			String asB64 = Base64.getEncoder()
					.encodeToString(baos.toByteArray());
			return asB64;
		}

		public static String writeObjectAsBase64URL(Object object)
				throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			oos.close();
			String asB64 = Base64.getUrlEncoder()
					.encodeToString(baos.toByteArray());
			return asB64;
		}

		public static void writeStreamToStream(InputStream is, OutputStream os)
				throws IOException {
			writeStreamToStream(is, os, false);
		}

		public static void writeStreamToStream(InputStream in, OutputStream os,
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

		public static void writeStringToFile(String s, File f)
				throws IOException {
			writeStringToOutputStream(s, new FileOutputStream(f));
		}

		public static void writeStringToFile(String s, String filename)
				throws IOException {
			writeStringToOutputStream(s, new FileOutputStream(filename));
		}

		public static void writeStringToFileGz(String s, File f)
				throws IOException {
			OutputStreamWriter fw = new OutputStreamWriter(
					new GZIPOutputStream(new FileOutputStream(f)), "UTF-8");
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(s);
			bw.close();
		}

		public static void writeStringToFileNoUpdate(String content,
				String path) throws Exception {
			if (!new File(path).exists()) {
				writeStringToFile(content, path);
				return;
			}
			String current = ReadOp.readFileToString(path);
			if (!current.equals(content)) {
				writeStringToFile(content, path);
			}
		}

		public static InputStream writeStringToInputStream(String s)
				throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter fw = new OutputStreamWriter(baos, "UTF-8");
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(s);
			bw.close();
			return new ByteArrayInputStream(baos.toByteArray());
		}

		public static void writeStringToOutputStream(String s, OutputStream os)
				throws IOException {
			OutputStreamWriter fw = new OutputStreamWriter(os, "UTF-8");
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(s);
			bw.close();
		}
	}
}
