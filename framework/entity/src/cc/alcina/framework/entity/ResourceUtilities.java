/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyDescriptor;
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
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.base.Preconditions;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.rpc.StatusCodeException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;
import cc.alcina.framework.entity.persistence.mvcc.TransactionalCollection;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;

/**
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class ResourceUtilities {
	private static Map<String, String> customProperties = new ConcurrentHashMap<String, String>();

	private static boolean clientWithJvmProperties;

	private static ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

	private static Method getCallerClassMethod;

	public static void appShutdown() {
	}

	public static StringMap classPathStringExistenceMap(Class clazz,
			String path) {
		return StringMap
				.fromStringList(readClassPathResourceAsString(clazz, path));
	}

	public static StringMap classPathStringMap(Class clazz, String path) {
		return StringMap
				.fromPropertyString(readClassPathResourceAsString(clazz, path));
	}

	public static <T> T copyBeanProperties(Object srcBean, T tgtBean,
			Class methodFilterAnnotation, boolean cloneCollections) {
		return copyBeanProperties(srcBean, tgtBean, methodFilterAnnotation,
				cloneCollections, new ArrayList<String>());
	}

	public static <T> T copyBeanProperties(Object srcBean, T tgtBean,
			Class methodFilterAnnotation, boolean cloneCollections,
			Collection<String> ignorePropertyNames) {
		for (PropertyDescriptor targetDescriptor : SEUtilities
				.getSortedPropertyDescriptors(tgtBean.getClass())) {
			if (ignorePropertyNames.contains(targetDescriptor.getName())) {
				continue;
			}
			PropertyDescriptor sourceDescriptor = SEUtilities
					.getPropertyDescriptorByName(srcBean.getClass(),
							targetDescriptor.getName());
			if (sourceDescriptor == null) {
				continue;
			}
			Method readMethod = sourceDescriptor.getReadMethod();
			if (readMethod == null) {
				continue;
			}
			if (methodFilterAnnotation != null) {
				if (readMethod.isAnnotationPresent(methodFilterAnnotation)) {
					continue;
				}
			}
			Method setMethod = targetDescriptor.getWriteMethod();
			if (setMethod != null) {
				try {
					Object obj = readMethod.invoke(srcBean, (Object[]) null);
					if (cloneCollections && obj instanceof Collection
							&& obj instanceof Cloneable) {
						Method clone = obj.getClass().getMethod("clone",
								new Class[0]);
						clone.setAccessible(true);
						obj = clone.invoke(obj, CommonUtils.EMPTY_OBJECT_ARRAY);
					}
					setMethod.invoke(tgtBean, obj);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
		return tgtBean;
	}

	public static DOMParser createDOMParser(boolean elementNamesToLowerCase) {
		DOMParser parser = new DOMParser();
		try {
			parser.setFeature(
					"http://cyberneko.org/html/features/scanner/fix-mswindows-refs",
					true);
			parser.setFeature(
					"http://cyberneko.org/html/features/scanner/ignore-specified-charset",
					true);
			if (elementNamesToLowerCase) {
				parser.setProperty(
						"http://cyberneko.org/html/properties/names/elems",
						"lower");
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return parser;
	}

	public static <T> T deserializeKryoOrAlcina(String string, Class<T> clazz) {
		try {
			return KryoUtils.deserializeFromBase64(string, clazz);
		} catch (Exception e) {
			try {
				return new AlcinaBeanSerializerS().deserialize(string);
			} catch (RuntimeException e1) {
				Ax.err(SEUtilities.getMessageOrClass(e));
				Ax.err(SEUtilities.getMessageOrClass(e1));
				throw e1;
			}
		}
	}

	public static void ensureFromSystemProperties() {
		String property = System.getProperty("ResourceUtilities.propertyPath");
		if (property != null) {
			try {
				registerCustomProperties(new FileInputStream(property));
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public static <T> T fieldwiseClone(T t) {
		return fieldwiseClone(t, false, false);
	}

	public static <T> T fieldwiseClone(T t, boolean withTransients,
			boolean withCollectionProjection) {
		try {
			T instance = newInstanceForCopy(t);
			return fieldwiseCopy(t, instance, withTransients,
					withCollectionProjection);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T fieldwiseCopy(T t, T toInstance, boolean withTransients,
			boolean withShallowCopiedCollections) {
		return fieldwiseCopy(t, toInstance, withTransients,
				withShallowCopiedCollections, null);
	}

	public static <T> T fieldwiseCopy(T t, T toInstance, boolean withTransients,
			boolean withShallowCopiedCollections,
			Set<String> ignoreFieldNames) {
		try {
			List<Field> allFields = new ArrayList<Field>();
			Class c = t.getClass();
			while (c != Object.class) {
				Field[] fields = c.getDeclaredFields();
				for (Field field : fields) {
					if (Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					if (Modifier.isFinal(field.getModifiers())) {
						continue;
					}
					if (Modifier.isTransient(field.getModifiers())
							&& !withTransients) {
						continue;
					}
					if (ignoreFieldNames != null
							&& ignoreFieldNames.contains(field.getName())) {
						continue;
					}
					field.setAccessible(true);
					allFields.add(field);
				}
				c = c.getSuperclass();
			}
			for (Field field : allFields) {
				Object value = field.get(t);
				boolean project = false;
				if (value != null && withShallowCopiedCollections) {
					if (value instanceof Map || value instanceof Collection) {
						project = !(value instanceof TransactionalCollection);
					}
				}
				if (project) {
					if (value instanceof Map) {
						Map map = (Map) value;
						Map newMap = (Map) map.getClass().newInstance();
						newMap.putAll(map);
						value = newMap;
					} else {
						Collection collection = (Collection) value;
						Collection newCollection = (Collection) newInstanceForCopy(
								collection);
						if (newCollection instanceof LiSet) {
							// handled by newInstanceForCopy/clone
						} else {
							newCollection.addAll(collection);
						}
						Preconditions.checkState(
								collection.size() == newCollection.size());
						value = newCollection;
					}
				}
				field.set(toInstance, value);
			}
			return toInstance;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String get(Class clazz, String propertyName) {
		return getBundledString(clazz, propertyName);
	}

	public static String get(String propertyName) {
		return get(getCallerClass(), propertyName);
	}

	public static boolean getBoolean(Class clazz, String propertyName) {
		String s = getBundledString(clazz, propertyName);
		return Boolean.valueOf(s);
	}

	public static BufferedImage getBufferedImage(Class clazz,
			String relativePath) {
		BufferedImage img = null;
		if (img == null) {
			try {
				img = ImageIO.read(clazz.getResource(relativePath));
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return img;
	}

	public static synchronized String getBundledString(Class clazz,
			String propertyName) {
		String namespacedKey = (clazz == null) ? propertyName
				: GraphProjection.classSimpleName(clazz) + "." + propertyName;
		return cache.computeIfAbsent(namespacedKey,
				k -> ResourceUtilities.getBundledString0(clazz, propertyName));
	}

	public static synchronized String getBundledString0(Class clazz,
			String propertyName) {
		String namespacedKey = (clazz == null) ? propertyName
				: GraphProjection.classSimpleName(clazz) + "." + propertyName;
		if (customProperties.containsKey(namespacedKey)) {
			return customProperties.get(namespacedKey);
		}
		try {
			if (GWT.isClient() && !isClientWithJvmProperties()) {
				return null;
			}
		} catch (Throwable t) {
			// suppress, no gwt on classpath
		}
		ResourceBundle b = null;
		b = ResourceBundle.getBundle(clazz.getPackage().getName() + ".Bundle",
				Locale.getDefault(), clazz.getClassLoader());
		if (b.keySet().contains(namespacedKey)) {
			return b.getString(namespacedKey);
		}
		return b.getString(propertyName);
	}

	public static Map<String, String> getCustomProperties() {
		return customProperties;
	}

	public static int getInteger(Class clazz, String propertyName) {
		return Integer.valueOf(getBundledString(clazz, propertyName));
	}

	public static int getInteger(Class clazz, String propertyName,
			int defaultValue) {
		try {
			String s = getBundledString(clazz, propertyName);
			return Integer.valueOf(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static long getLong(Class clazz, String key) {
		return Long.parseLong(get(clazz, key));
	}

	public static byte[] gunzipBytes(byte[] bytes) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPInputStream gzipInputStream = new GZIPInputStream(
					new ByteArrayInputStream(bytes));
			writeStreamToStream(gzipInputStream, baos);
			return baos.toByteArray();
		} catch (Exception e) {
			return bytes;
		}
	}

	public static byte[] gzipBytes(byte[] bytes) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);
			gzipOutputStream.write(bytes);
			gzipOutputStream.flush();
			gzipOutputStream.close();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static boolean is(Class clazz, String propertyName) {
		return getBoolean(clazz, propertyName);
	}

	public static boolean is(String propertyName) {
		return is(getCallerClass(), propertyName);
	}

	public static boolean isClientWithJvmProperties() {
		return clientWithJvmProperties;
	}

	public static boolean isDefined(String key) {
		return customProperties.containsKey(key);
	}

	public static Document loadDocumentFromInputStream(InputStream is)
			throws Exception {
		return loadHtmlDocumentFromInputStream(is, null);
	}

	public static Document loadHtmlDocumentFromInputStream(InputStream is,
			String charset) throws Exception {
		byte[] bs = ResourceUtilities.readStreamToByteArray(is);
		is.close();
		InputSource isrc = null;
		if (charset == null) {
			isrc = new InputSource(new ByteArrayInputStream(bs));
		} else {
			isrc = new InputSource(new InputStreamReader(
					new ByteArrayInputStream(bs), charset));
		}
		DOMParser parser = createDOMParser(false);
		parser.parse(isrc);
		return (Document) parser.getDocument();
	}

	public static Document loadHtmlDocumentFromString(String s)
			throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
		osw.write(s);
		osw.close();
		return loadHtmlDocumentFromInputStream(
				new ByteArrayInputStream(baos.toByteArray()), "UTF-8");
	}

	public static Document loadHtmlDocumentFromUrl(String url) {
		try {
			return loadDocumentFromInputStream(new URL(url).openStream());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void loadSystemPropertiesFromCustomProperties() {
		Map<String, String> map = getCustomProperties();
		map.forEach((k, v) -> {
			if (k.startsWith("system.property.")) {
				k = k.substring("system.property.".length());
				System.setProperty(k, v);
			}
		});
	}

	public static DomDoc loadXmlDocFromHtmlString(String html) {
		try {
			return new DomDoc(loadHtmlDocumentFromString(html));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static DomDoc loadXmlDocFromUrl(String url) {
		return new DomDoc(loadHtmlDocumentFromUrl(url));
	}

	public static void logToFile(String content) {
		logToFile(content, "log.txt");
		logToFile(content, "log.html");
		logToFile(content, "log.xml");
	}

	public static void logToFile(String content, String fileName) {
		try {
			new File("/tmp/log").mkdirs();
			writeStringToFile(content, "/tmp/log/" + fileName);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static boolean not(Class clazz, String key) {
		return !is(clazz, key);
	}

	public static boolean notDisabled(Class clazz) {
		return !isDefined(clazz.getName() + ".disabled");
	}

	public static String objectOrPrimitiveToString(Object object) {
		if (object == null) {
			return null;
		}
		return object.toString();
	}

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

	public static String read(String path) {
		try {
			return readFileToString(path);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static byte[] readClassPathResourceAsByteArray(Class clazz,
			String path) {
		try {
			return readStreamToByteArray(clazz.getResourceAsStream(path));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String readClassPathResourceAsString(Class clazz,
			String path) {
		try {
			return readStreamToString(clazz.getResourceAsStream(path));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String readClassPathResourceAsStringPreferFile(Class clazz,
			String path, String filePath) {
		File file = new File(filePath);
		if (is(ResourceUtilities.class, "useDevResources") && file.exists()) {
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

	public static String readFileToString(String fileName) throws IOException {
		return readFileToString(new File(fileName));
	}

	public static String readFileToStringGz(File f) throws IOException {
		InputStream fis = new FileInputStream(f);
		if (f.getName().endsWith(".gz")) {
			fis = new GZIPInputStream(new BufferedInputStream(fis));
		}
		return readStreamToString(fis);
	}

	public static <T> T readObjectFromBase64(String string) throws IOException {
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
		return readClassPathResourceAsString(getCallerClass(), path);
	}

	public static byte[] readStreamToByteArray(InputStream is)
			throws IOException {
		int bufLength = is.available() <= 1024 ? 1024 * 64 : is.available();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(bufLength);
		writeStreamToStream(is, baos);
		return baos.toByteArray();
	}

	public static String readStreamToString(InputStream is) throws IOException {
		return readStreamToString(is, null);
	}

	public static String readStreamToString(InputStream is, String charsetName)
			throws IOException {
		try {
			byte[] bytes = readStreamToByteArray(is);
			charsetName = charsetName == null ? "UTF-8" : charsetName;
			return new String(bytes, charsetName);
		} finally {
			is.close();
		}
	}

	public static byte[] readUrlAsByteArray(String strUrl) throws IOException {
		URL url = new URL(strUrl);
		InputStream is = null;
		is = url.openConnection().getInputStream();
		return readStreamToByteArray(is);
	}

	public static byte[] readUrlAsBytesWithPost(String strUrl, String postBody,
			StringMap headers) throws Exception {
		return new SimpleQuery(strUrl).withPostBody(postBody)
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

	public static String readUrlAsStringWithPost(String strUrl, String postBody,
			StringMap headers) throws Exception {
		byte[] bytes = readUrlAsBytesWithPost(strUrl, postBody, headers);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public static void registerCustomProperties(InputStream ios) {
		try {
			Properties p = new Properties();
			if (ios != null) {
				boolean debugRegistration = customProperties != null
						&& customProperties.containsKey(
								"ResourceUtilities.debugPropertyRegistration")
						&& is(ResourceUtilities.class,
								"debugPropertyRegistration");
				if (debugRegistration) {
					Ax.out("--- debug registration --- %s", ios);
				}
				p.load(ios);
				if (debugRegistration) {
					Ax.out(p.entrySet());
				}
				ios.close();
				registerCustomProperties(p);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void registerCustomProperties(Properties p) {
		for (Entry<Object, Object> entry : p.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (!(key instanceof String) || !(value instanceof String)) {
				continue;
			}
			customProperties.put((String) key, (String) value);
			cache.clear();
		}
	}

	public static void registerCustomProperties(String path) {
		InputStream ios = ResourceUtilities.class.getResourceAsStream(path);
		registerCustomProperties(ios);
	}

	public static void registerCustomProperty(String key, String value) {
		ResourceUtilities.registerCustomProperties(new ByteArrayInputStream(
				Ax.format("%s=%s", key, value).getBytes()));
	}

	public static OutputStream scaleImage(InputStream in, int width, int height,
			OutputStream out) throws IOException {
		byte[] b = readStreamToByteArray(in);
		ImageIcon icon = new ImageIcon(b);
		Image image = icon.getImage();
		int thumbWidth = width;
		int thumbHeight = height;
		double thumbRatio = (double) thumbWidth / (double) thumbHeight;
		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);
		double imageRatio = (double) imageWidth / (double) imageHeight;
		if (thumbRatio < imageRatio) {
			thumbHeight = (int) (thumbWidth / imageRatio);
		} else {
			thumbWidth = (int) (thumbHeight * imageRatio);
		}
		// draw original image to thumbnail image object and
		// scale it to the new size on-the-fly
		BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = thumbImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
		// save thumbnail image to OUTFILE
		ImageIO.write(thumbImage, "png", out);
		return out;
	}

	public static Object serialClone(Object bean) {
		Object result = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeObject(bean);
			out.close();
			ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(baos.toByteArray()));
			result = in.readObject();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return result;
	}

	public static void set(Class<?> clazz, String key, String value) {
		set(Ax.format("%s.%s", clazz.getSimpleName(), key), value);
	}

	public static synchronized void set(String key, String value) {
		customProperties.put(key, value);
		cache.clear();
	}

	public static void
			setClientWithJvmProperties(boolean clientWithJvmProperties) {
		ResourceUtilities.clientWithJvmProperties = clientWithJvmProperties;
	}

	public static void setField(Object object, String fieldPath,
			Object newValue) throws Exception {
		Object cursor = object;
		Field field = null;
		String[] segments = fieldPath.split("\\.");
		for (int idx = 0; idx < segments.length; idx++) {
			String segment = segments[idx];
			field = SEUtilities.getFieldByName(cursor.getClass(), segment);
			field.setAccessible(true);
			if (idx < segments.length - 1) {
				cursor = field.get(cursor);
			} else {
				field.set(cursor, newValue);
			}
		}
	}

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

	public static String writeObjectAsBase64(Object object) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		oos.close();
		String asB64 = Base64.getEncoder().encodeToString(baos.toByteArray());
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

	public static void writeStringToFile(String s, File f) throws IOException {
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

	public static void writeStringToFileNoUpdate(String content, String path)
			throws Exception {
		if (!new File(path).exists()) {
			writeStringToFile(content, path);
			return;
		}
		String current = readFileToString(path);
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

	private static <T> T newInstanceForCopy(T t)
			throws NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		if (t instanceof LiSet) {
			return (T) ((LiSet) t).clone();
		}
		Constructor<T> constructor = null;
		try {
			constructor = (Constructor<T>) t.getClass()
					.getConstructor(new Class[0]);
		} catch (NoSuchMethodException e) {
			constructor = (Constructor<T>) t.getClass()
					.getDeclaredConstructor(new Class[0]);
		}
		constructor.setAccessible(true);
		T instance = constructor.newInstance();
		return instance;
	}

	protected static Class getCallerClass() {
		try {
			if (getCallerClassMethod == null) {
				Class clazz = Class.forName("sun.reflect.Reflection");
				getCallerClassMethod = clazz.getMethod("getCallerClass",
						int.class);
			}
			Class callerClass = (Class) getCallerClassMethod.invoke(null, 3);
			return callerClass;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static class SimpleQuery {
		private String strUrl;

		private String postBody;

		private StringMap headers = new StringMap();

		private HttpURLConnection connection;

		private boolean gzip;

		private boolean decodeGz;

		private String contentType;

		private String contentDisposition;

		private StringMap queryStringParameters;

		public SimpleQuery(String strUrl) {
			this.strUrl = strUrl;
		}

		public byte[] asBytes() throws Exception {
			InputStream in = null;
			connection = null;
			if (headers == null) {
				headers = new StringMap();
			}
			if (queryStringParameters != null) {
				if (!strUrl.contains("?")) {
					strUrl += "?";
				}
				strUrl += queryStringParameters.entrySet().stream().map(e -> {
					return Ax.format("%s=%s", e.getKey(),
							UrlComponentEncoder.get().encode(e.getValue()));
				}).collect(Collectors.joining("&"));
			}
			try {
				URL url = new URL(strUrl);
				connection = (HttpURLConnection) (url.openConnection());
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setUseCaches(false);
				if (postBody != null) {
					connection.setRequestMethod("POST");
				}
				if (gzip) {
					headers.put("accept-encoding", "gzip");
				}
				for (Entry<String, String> e : headers.entrySet()) {
					connection.setRequestProperty(e.getKey(), e.getValue());
				}
				if (postBody != null) {
					OutputStream out = connection.getOutputStream();
					Writer wout = new OutputStreamWriter(out, "UTF-8");
					wout.write(postBody);
					wout.flush();
					wout.close();
				}
				in = connection.getInputStream();
				byte[] input = readStreamToByteArray(in);
				int responseCode = connection.getResponseCode();
				if (responseCode >= 400) {
					throw new StatusCodeException(responseCode,
							connection.getResponseMessage());
				}
				if (decodeGz) {
					input = maybeDecodeGzip(input);
				}
				contentType = connection.getContentType();
				contentDisposition = connection
						.getHeaderField("Content-Disposition");
				return input;
			} catch (IOException ioe) {
				if (connection != null) {
					InputStream err = connection.getErrorStream();
					String errString = null;
					if (err != null) {
						byte[] input = readStreamToByteArray(err);
						if (decodeGz) {
							input = maybeDecodeGzip(input);
						}
						errString = new String(input, StandardCharsets.UTF_8);
					}
					throw new IOException(errString, ioe);
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

		public String asString() throws Exception {
			return new String(asBytes(), StandardCharsets.UTF_8);
		}

		public void echo() {
			try {
				Ax.out(asString());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public String getContentDisposition() {
			return this.contentDisposition;
		}

		public String getContentType() {
			return this.contentType;
		}

		public SimpleQuery withBasicAuthentication(String username,
				String password) {
			String auth = Ax.format("%s:%s", username, password);
			headers.put("Authorization",
					Ax.format("Basic %s", Base64.getEncoder().encodeToString(
							auth.getBytes(StandardCharsets.UTF_8))));
			return this;
		}

		public SimpleQuery withDecodeGz(boolean decodeGz) {
			this.decodeGz = decodeGz;
			return this;
		}

		public SimpleQuery withGzip(boolean gzip) {
			this.gzip = gzip;
			return this;
		}

		public SimpleQuery withHeaders(StringMap headers) {
			this.headers = headers;
			return this;
		}

		public SimpleQuery withPostBody(String postBody) {
			this.postBody = postBody;
			return this;
		}

		public SimpleQuery
				withPostBodyQueryParameters(StringMap queryParameters) {
			postBody = queryParameters.entrySet().stream().map(e -> {
				return Ax.format("%s=%s", e.getKey(),
						UrlComponentEncoder.get().encode(e.getValue()));
			}).collect(Collectors.joining("&"));
			headers.put("content-type", "application/x-www-form-urlencoded");
			return this;
		}

		public SimpleQuery
				withQueryStringParameters(StringMap queryStringParameters) {
			this.queryStringParameters = queryStringParameters;
			return this;
		}

		private byte[] maybeDecodeGzip(byte[] input) throws IOException {
			if ("gzip".equals(connection.getHeaderField("content-encoding"))) {
				return readStreamToByteArray(
						new GZIPInputStream(new ByteArrayInputStream(input)));
			} else {
				return input;
			}
		}
	}
}
