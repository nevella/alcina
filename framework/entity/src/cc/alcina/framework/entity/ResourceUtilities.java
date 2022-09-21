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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.base.Preconditions;
import com.google.gwt.core.shared.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.persistence.mvcc.TransactionalCollection;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;

/**
 * @author nick@alcina.cc
 *
 *         FIXME - 2022 - migrate property treatment to Configuration static
 *         singleton
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class ResourceUtilities {
	private static Map<String, String> customProperties = new ConcurrentHashMap<String, String>();

	private static boolean clientWithJvmProperties;

	private static Map<String, String> cache = new ConcurrentHashMap<>();

	public static final Topic<Void> propertiesInvalidated = Topic.create();

	private static Set<String> immutableCustomProperties = new LinkedHashSet<>();

	static Logger logger = LoggerFactory.getLogger(ResourceUtilities.class);

	/*
	 * Security-related properties that should not be settable post-startup
	 */
	public static void addImmutableCustomPropertyKey(String key) {
		immutableCustomProperties.add(key);
	}

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
				.getPropertyDescriptorsSortedByName(tgtBean.getClass())) {
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
			List<Field> fields = getFieldsForCopyOrLog(t, withTransients,
					ignoreFieldNames);
			for (Field field : fields) {
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
						Map newMap = (Map) map.getClass()
								.getDeclaredConstructor().newInstance();
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
		return get(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), propertyName);
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
		return is(StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass(), propertyName);
	}

	public static boolean isClientWithJvmProperties() {
		return clientWithJvmProperties;
	}

	public static boolean isDefined(String key) {
		return customProperties.containsKey(key);
	}

	public static Document loadDocumentFromInputStream(InputStream is)
			throws Exception {
		return loadHtmlDocumentFromInputStream(is, null, true);
	}

	public static Document loadHtmlDocumentFromInputStream(InputStream stream,
			String charset, boolean upperCaseTags) throws Exception {
		stream = maybeWrapInBufferedStream(stream);
		InputSource isrc = null;
		if (charset == null) {
			isrc = new InputSource(stream);
		} else {
			isrc = new InputSource(new InputStreamReader(stream, charset));
		}
		DOMParser parser = createDOMParser(upperCaseTags);
		parser.parse(isrc);
		return (Document) parser.getDocument();
	}

	public static Document loadHtmlDocumentFromString(String s,
			boolean upperCaseTags) throws Exception {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_16);
		return loadHtmlDocumentFromInputStream(new ByteArrayInputStream(bytes),
				"UTF-16", upperCaseTags);
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

	public static DomDocument loadXmlDocFromHtmlStream(InputStream stream,
			int size, boolean upperCaseTags) {
		try {
			return new DomDocument(loadHtmlDocumentFromInputStream(stream, null,
					upperCaseTags), size);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static DomDocument loadXmlDocFromHtmlString(String html,
			boolean upperCaseTags) {
		try {
			return new DomDocument(
					loadHtmlDocumentFromString(html, upperCaseTags),
					html.length());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static DomDocument loadXmlDocFromUrl(String url) {
		return new DomDocument(loadHtmlDocumentFromUrl(url));
	}

	public static void logToFile(String content) {
		logToFile(content, "log.txt");
		logToFile(content, "log.html");
		logToFile(content, "log.xml");
	}

	public static void logToFile(String content, String fileName) {
		try {
			new File("/tmp/log").mkdirs();
			String path = "/tmp/log/" + fileName;
			writeStringToFile(content, path);
			Ax.out("Logged to: %s", path);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static InputStream maybeWrapInBufferedStream(InputStream stream) {
		if (stream instanceof FileInputStream) {
			return new BufferedInputStream(stream, 64 * 1024);
		} else {
			return stream;
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

	public static Map<String, String> primitiveFieldValues(Object t) {
		try {
			Map<String, String> map = new LinkedHashMap<>();
			List<Field> fields = getFieldsForCopyOrLog(t, false, null);
			for (Field field : fields) {
				if (GraphProjection.isPrimitiveOrDataClass(field.getType())) {
					Object value = field.get(t);
					map.put(field.getName(), String.valueOf(value));
				}
			}
			return map;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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

	public static String readUrlAsStringWithPost(String strUrl, String postBody,
			StringMap headers) throws Exception {
		byte[] bytes = readUrlAsBytesWithPost(strUrl, postBody, headers);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public static String readUrlAsStringWithPut(String strUrl, String body,
			StringMap headers) throws Exception {
		byte[] bytes = readUrlAsBytesWithPut(strUrl, body, headers);
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
			set((String) key, (String) value);
		}
		clearCacheAndFireChange();
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

	public static String set(String key, String value) {
		if (immutableCustomProperties.contains(key)) {
			logger.info("Not updating immutable property: {}", key);
			return null;
		}
		String existing = customProperties.get(key);
		customProperties.put(key, value);
		clearCacheAndFireChange();
		return existing;
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

	private static void clearCacheAndFireChange() {
		cache.clear();
		propertiesInvalidated.publish(null);
	}

	private static DOMParser createDOMParser(boolean elementNamesToUpperCase) {
		DOMParser parser = new DOMParser(new HTMLConfiguration());
		try {
			parser.setFeature(
					"http://cyberneko.org/html/features/scanner/fix-mswindows-refs",
					true);
			parser.setFeature(
					"http://cyberneko.org/html/features/scanner/ignore-specified-charset",
					true);
			if (!elementNamesToUpperCase) {
				parser.setProperty(
						"http://cyberneko.org/html/properties/names/elems",
						"lower");
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return parser;
	}

	private static InputStream getResourceAsStream(Class clazz, String path) {
		InputStream stream = clazz.getResourceAsStream(path);
		if (stream == null) {
			stream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(path);
		}
		return stream;
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

	protected static <T> List<Field> getFieldsForCopyOrLog(T t,
			boolean withTransients, Set<String> ignoreFieldNames) {
		List<Field> result = new ArrayList<>();
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
				result.add(field);
			}
			c = c.getSuperclass();
		}
		return result;
	}

	/*
	 * Doesn't do a defensive copy of the internal byte array when calling
	 * toByteArray (so toByteArray can only be used once, as the last operation
	 * on the instance)
	 */
	static class DisposableByteArrayOutputStream extends ByteArrayOutputStream {
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
