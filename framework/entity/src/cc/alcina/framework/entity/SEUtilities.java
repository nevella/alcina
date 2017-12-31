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

import java.awt.Component;
import java.awt.Container;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.HasAnnotationCallback;
import cc.alcina.framework.common.client.logic.reflection.NoSuchPropertyException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.IidGenerator;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.util.JvmPropertyReflector;

/**
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class SEUtilities {
	public static int idCounter = 1;

	private static Pattern yearRangePattern = Pattern
			.compile("(\\d{4})(-(\\d{4}))?");

	private static UnsortedMultikeyMap<PropertyDescriptor> pdLookup = new UnsortedMultikeyMap<PropertyDescriptor>(
			2);

	private static Map<Class, List<Field>> allFieldsPerClass = new LinkedHashMap<>();

	private static Pattern sq_1 = Pattern.compile("(?<=\\s|^)\"");

	private static Pattern sq_2 = Pattern.compile("(?<=\\S)\"");

	private static Pattern sq_3 = Pattern.compile("(?<=\\s|^)[`'´]");

	private static Pattern sq_4 = Pattern.compile("(?<=\\S|^)[`'´]");

	private static Pattern sq_5 = Pattern.compile("[`'´]+");

	private static Pattern sq_6 = Pattern.compile("[`'´]{2,}");

	public static List<Field> allFields(Class clazz0) {
		return allFieldsPerClass.computeIfAbsent(clazz0, clazz -> {
			List<Field> result = new ArrayList<>();
			try {
				while (clazz != Object.class) {
					for (Field f : clazz.getDeclaredFields()) {
						f.setAccessible(true);
						result.add(f);
					}
					clazz = clazz.getSuperclass();
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			return result;
		});
	}

	public static void appShutdown() {
		pdLookup = null;
	}

	public static void clearAllFields(Object object) {
		try {
			List<Field> fields = allFields(object.getClass());
			Object template = object.getClass().newInstance();
			for (Field field : fields) {
				int modifiers = field.getModifiers();
				if (Modifier.isFinal(modifiers)
						|| Modifier.isStatic(modifiers)) {
					continue;
				} else {
					field.set(object, field.get(template));
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <C> C collectionItemOfClass(Collection coll, Class<C> clazz) {
		for (Object object : coll) {
			if (object.getClass() == clazz) {
				return (C) object;
			}
		}
		return null;
	}

	// assume slash-delineated
	public static String combinePaths(String absPath, String relPath) {
		if (relPath.contains("://")) {
			return relPath;
		}
		if (relPath.startsWith("/")) {
			if (absPath.contains("://")) {
				int idx0 = absPath.indexOf("://") + 3;
				int idx1 = absPath.indexOf("/", idx0);
				return (idx1 == -1 ? absPath : absPath.substring(0, idx1))
						+ relPath;
			} else {
				return relPath;
			}
		}
		String parentSep = "../";
		String voidSep = "./";
		int x = 0;
		x = absPath.lastIndexOf("/");
		if (x != -1) {
			absPath = absPath.substring(0, x);
		}
		while (relPath.startsWith(parentSep)) {
			x = absPath.lastIndexOf("/");
			absPath = absPath.substring(0, x);
			relPath = relPath.substring(parentSep.length());
		}
		if (relPath.startsWith(voidSep)) {
			relPath = relPath.substring(voidSep.length());
		}
		if (!absPath.endsWith("/")) {
			absPath += "/";
		}
		return absPath + relPath;
	}

	public static String consoleReadline(String prompt) {
		System.out.print(prompt);
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);
		try {
			return in.readLine();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String constantToDashedLc(Object constant) {
		return constant.toString().replace('_', '-').toLowerCase();
	}

	public static boolean containsDodgyAscii(String text) {
		for (int idx = 0; idx < text.length(); idx++) {
			int codePoint = (int) text.charAt(idx);
			if (codePoint >= 0x80 && codePoint <= 0x9f) {
				return true;
			}
		}
		return false;
	}

	public static int copyFile(File in, File out) throws IOException {
		return copyFile(in, out, false);
	}

	public static int copyFile(File in, File out, boolean forceOverwrite)
			throws IOException {
		if (in.isDirectory()) {
			return copyDirectory(in, out);
		}
		if (!out.exists()) {
			out.getParentFile().mkdirs();
			out.createNewFile();
		} else {
			if (out.lastModified() >= in.lastModified()
					&& out.length() == in.length() && !forceOverwrite) {
				return 0;
			}
		}
		FileInputStream ins = new FileInputStream(in);
		FileOutputStream os = new FileOutputStream(out);
		ResourceUtilities.writeStreamToStream(ins, os);
		out.setLastModified(in.lastModified());
		ins.close();
		return 1;
	}

	public static <T> void copyProperties(T from, T to,
			String... propertyNames) {
		try {
			for (String propertyName : propertyNames) {
				PropertyDescriptor pd = getPropertyDescriptorByName(
						from.getClass(), propertyName);
				pd.getWriteMethod().invoke(to, pd.getReadMethod().invoke(from));
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String decUtf8(String s) {
		try {
			return s == null ? null : URLDecoder.decode(s, "UTF-8");
		} catch (Exception e) {
			// ahhh - doesn't happen much
			throw new WrappedRuntimeException(e);
		}
	}

	public static boolean deleteDirectory(File folder) {
		if (!folder.exists()) {
			return false;
		}
		if (!folder.isDirectory()) {
			return folder.delete();
		}
		Stack<File> pathStack = new Stack<File>();
		Stack<File> dirStack = new Stack<File>();
		pathStack.push(folder);
		dirStack.push(folder);
		while (pathStack.size() != 0) {
			File dInf = pathStack.pop();
			File[] files = dInf.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					pathStack.push(file);
					dirStack.push(file);
				} else {
					boolean b = file.delete();
					if (!b) {
						return b;
					}
				}
			}
		}
		while (dirStack.size() != 0) {
			File dInf = dirStack.pop();
			boolean b = dInf.delete();
			if (!b) {
				return b;
			}
		}
		return true;
	}

	public static void disableSslValidation() throws Exception {
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public void checkClientTrusted(X509Certificate[] certs,
							String authType) {
					}

					public void checkServerTrusted(X509Certificate[] certs,
							String authType) {
					}

					public java.security.cert.X509Certificate[]
							getAcceptedIssuers() {
						return null;
					}
				} };
		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	public static void dump(List list) {
		System.out.println("List:");
		for (Object object : list) {
			System.out.println("\t- " + object);
		}
	}

	public static void dumpBytes(byte[] bs, int width) {
		dumpBytes(bs, width, true);
	}

	public static void dumpBytes(byte[] bs, int width, boolean indexAsHex) {
		StringBuilder bd = new StringBuilder();
		int len = bs.length;
		for (int i = 0; i < len; i += width) {
			bd.append(CommonUtils.padStringLeft(
					(indexAsHex ? Integer.toHexString(i) : String.valueOf(i)),
					8, '0'));
			bd.append(":  ");
			for (int j = 0; j < width; j++) {
				boolean in = j + i < len;
				// int rather than byte so we can unsign
				int b = in ? bs[i + j] : 0;
				if (b < 0) {
					b += 256;
				}
				bd.append(in ? CommonUtils.padStringLeft(Integer.toHexString(b),
						2, '0') : "  ");
				bd.append("  ");
			}
			for (int j = 0; j < width; j++) {
				boolean in = j + i < len;
				char c = in ? (char) bs[i + j] : ' ';
				c = c < '\u0020' || c >= '\u007F' ? '.' : c;
				bd.append(c);
			}
			bd.append('\n');
		}
		System.out.println(bd.toString());
	}

	public static void dumpChars(String s, int width) {
		StringBuilder bd = new StringBuilder();
		int len = s.length();
		for (int i = 0; i < len; i += width) {
			bd.append(CommonUtils.padStringLeft(Integer.toString(i), 8, '0'));
			bd.append(":  ");
			for (int j = 0; j < width; j++) {
				boolean in = j + i < len;
				bd.append(in ? s.charAt(j + i) : ' ');
			}
			bd.append('\n');
		}
		System.out.println(bd.toString());
	}

	public static String dumpProperties(Properties p) {
		StringWriter sw = new StringWriter();
		sw.write("--listing properties--\n");
		for (Enumeration<?> names = p.propertyNames(); names
				.hasMoreElements();) {
			String name = (String) names.nextElement();
			sw.write(name + "=" + p.getProperty(name) + "\n");
		}
		return sw.toString();
	}

	public static void dumpStringBytes(String s) {
		try {
			dumpBytes(s.getBytes("UTF-16"), 8);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String encodePath(String path) throws Exception {
		StringTokenizer t = new StringTokenizer(path, "/");
		boolean isFirst = true;
		StringBuffer buffer = new StringBuffer();
		while (t.hasMoreElements()) {
			if (isFirst) {
				isFirst = false;
			} else {
				buffer.append("/");
			}
			buffer.append(URLEncoder.encode(t.nextToken(), "UTF-8"));
		}
		return buffer.toString();
	}

	public static String encUtf8(String s) {
		try {
			return s == null ? null : URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			// ahhh - doesn't happen much
			throw new WrappedRuntimeException(e);
		}
	}

	public static void ensureLogFolder() {
		new File("/tmp/log").mkdirs();
	}

	public static Map<String, String> enumToMap(Enum e) {
		Map<String, String> m = new HashMap<String, String>();
		try {
			Method method = e.getClass().getMethod("values", new Class[0]);
			Object[] values = (Object[]) method.invoke(e,
					CommonUtils.EMPTY_OBJECT_ARRAY);
			for (Object o : values) {
				String s = o.toString();
				m.put(s, s);
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		return m;
	}

	public static boolean equivalentDeclaredFields(Object o1, Object o2) {
		if (o1 == null || o2 == null || o1.getClass() != o2.getClass()) {
			return false;
		}
		try {
			Class clazz = o1.getClass();
			while (clazz != Object.class) {
				for (Field f : clazz.getDeclaredFields()) {
					f.setAccessible(true);
					if (!CommonUtils.equalsWithNullEmptyEquality(f.get(o1),
							f.get(o2))) {
						return false;
					}
				}
				clazz = clazz.getSuperclass();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return true;
	}

	public static void expandAll(JTree tree, boolean expand) {
		TreeNode root = (TreeNode) tree.getModel().getRoot();
		// Traverse tree from root
		expandAll(tree, new TreePath(root), expand);
	}

	public static <T> List<T> filterCollection(Collection<T> coll,
			ObjectFilter<T> filter) {
		List<T> result = new ArrayList<T>();
		for (Iterator<T> itr = coll.iterator(); itr.hasNext();) {
			T next = itr.next();
			if (filter.include(next)) {
				result.add(next);
			}
		}
		return result;
	}

	public static Component findComponentOfClass(Container parent,
			Class childClass) {
		Stack<Container> containerStack = new Stack<Container>();
		containerStack.push(parent);
		while (containerStack.size() != 0) {
			Container c = containerStack.pop();
			Component[] components = c.getComponents();
			for (Component component : components) {
				if (component.getClass() == childClass) {
					return component;
				}
				if (component instanceof Container) {
					Container container = (Container) component;
					containerStack.push(container);
				}
			}
		}
		return null;
	}

	// TODO not implemented
	public static Component findComponentOfName(Container parent,
			String childName) {
		Component retVal = null;
		return retVal;
	}

	public static String forHTMLTextFlow(String aText) {
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = new StringCharacterIterator(
				aText);
		char character = iterator.current();
		while (character != CharacterIterator.DONE) {
			if (character == '<') {
				result.append("&lt;");
			} else if (character == '>') {
				result.append("&gt;");
			} else if (character == '\"') {
				result.append("&quot;");
			} else if (character == '\'') {
				result.append("&#039;");
			} else if (character == '&') {
				result.append("&amp;");
			} else {
				// the char is not a special one
				// add it to the result as is
				result.append(character);
			}
			character = iterator.next();
		}
		return result.toString();
	}

	public static String friendlyClassName(Class clazz) {
		String sc = clazz.getSimpleName();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sc.length(); i++) {
			char c = sc.charAt(i);
			if (Character.isUpperCase(c) && sb.length() != 0) {
				sb.append(' ');
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String generateId() {
		char[][] ranges = { { 'a', 'z' }, { 'A', 'Z' }, { '0', '9' },
				{ '_', '_' } };
		return generateId(ranges, 32);
	}

	public static String generateId(char[][] ranges, int length) {
		StringBuffer sb = new StringBuffer();
		Random r = new Random();
		int maxV = 0;
		for (int i = 0; i < ranges.length; i++) {
			maxV += ranges[i][1] - ranges[i][0] + 1;
		}
		List<Integer> ints = new ArrayList<Integer>(64);
		int j = idCounter++;
		while (j != 0) {
			ints.add(j % maxV);
			j /= maxV;
		}
		long l = System.currentTimeMillis();
		while (l != 0) {
			ints.add((int) (l - (l / maxV) * maxV));
			l /= maxV;
		}
		ints.add(idCounter++);
		for (int i = 0; i < length; i++) {
			ints.add(r.nextInt(maxV));
		}
		for (int k : ints) {
			int fk = k;
			for (int i = 0; i < ranges.length; i++) {
				int dv = ranges[i][1] - ranges[i][0] + 1;
				if (k < dv) {
					sb.append((char) (k + (int) ranges[i][0]));
					break;
				}
				k -= dv;
			}
		}
		String string = sb.toString();
		int pre = length / 2;
		return string.substring(0, pre)
				+ string.substring(string.length() - length + pre);
	}

	public static String getAccessorName(Field field) {
		return (field.getType() == boolean.class ? "is" : "get")
				+ CommonUtils.capitaliseFirst(field.getName());
	}

	public static Calendar getCalendarRoundedToDay() {
		Calendar cal = Calendar.getInstance();
		cal.clear(Calendar.MILLISECOND);
		cal.clear(Calendar.SECOND);
		cal.clear(Calendar.MINUTE);
		cal.clear(Calendar.HOUR);
		return cal;
	}

	public static File getChildFile(File folder, String childFileName) {
		return new File(
				String.format("%s/%s", folder.getPath(), childFileName));
	}

	public static File getDesktopFolder() {
		switch (getOsType()) {
		case Windows:
		case MacOS:
		case Unix:
			File file = new File(System.getProperty("user.home")
					+ File.separator + "Desktop");
			return (file.exists()) ? file
					: new File(System.getProperty("user.home"));
		default:
			return null;
		}
	}

	public static Field getFieldByName(Class clazz, String name) {
		return allFields(clazz).stream().filter(f -> f.getName().equals(name))
				.findFirst().orElse(null);
	}

	public static String getFullExceptionMessage(Throwable t) {
		StringWriter sw = new StringWriter();
		sw.write(t.getMessage() + "\n");
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	public static String getHomeDir() {
		return (System.getenv("USERPROFILE") != null)
				? System.getenv("USERPROFILE")
				: System.getProperty("user.home");
	}

	public static int getLeadingWsCount(String input) {
		int sct = 0;
		itrChars: for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch (c) {
			case '\u0009':
			case '\n':
			case '\u000B':
			case '\f':
			case '\r':
			case '\u00A0':
			case ' ':
				sct++;
				break;
			default:
				break itrChars;
			}
		}
		return sct;
	}

	public static String getNameFromPath(String path) {
		path = path.replace('\\', '/');
		return (path.contains("/")) ? path.substring(path.lastIndexOf("/") + 1)
				: path;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getOrCreate(Collection<T> existing, String propertyName,
			String propertyValue, Class itemClass) throws Exception {
		PropertyDescriptor descriptor = getPropertyDescriptorByName(itemClass,
				propertyName);
		for (Iterator<T> itr = existing.iterator(); itr.hasNext();) {
			T item = itr.next();
			if (propertyValue.equals(descriptor.getReadMethod().invoke(item,
					CommonUtils.EMPTY_OBJECT_ARRAY))) {
				return item;
			}
		}
		Object item = itemClass.newInstance();
		setPropertyValue(item, propertyName, propertyValue);
		return (T) item;
	}

	public static OsType getOsType() {
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Mac OS")) {
			return OsType.MacOS;
		} else if (osName.startsWith("Windows")) {
			return OsType.Windows;
		} else {
			return OsType.Unix;
		}
	}

	public static String getParentPath(String path) {
		if (path.contains("/")) {
			return path.substring(0, path.lastIndexOf("/"));
		}
		return "";
	}

	public static Map<String, Object> getPropertiesAsMap(Object obj,
			List<String> ignore) {
		try {
			getPropertyDescriptorByName(obj.getClass(), null);
			Map<String, PropertyDescriptor> pds = (Map<String, PropertyDescriptor>) pdLookup
					.asMap(obj.getClass()).delegate();
			Map<String, Object> props = new LinkedHashMap<String, Object>();
			for (PropertyDescriptor pd : pds.values()) {
				if (!ignore.contains(pd.getName())) {
					props.put(pd.getName(), pd.getReadMethod() == null ? null
							: pd.getReadMethod().invoke(obj));
				}
			}
			return props;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static PropertyDescriptor getPropertyDescriptorByName(Class clazz,
			String propertyName) {
		ensureDescriptorLookup(clazz);
		PropertyDescriptor cached = pdLookup.get(clazz, propertyName);
		return cached;
	}

	public static Object getPropertyValue(Object bean, String propertyName) {
		try {
			return getPropertyDescriptorByName(bean.getClass(), propertyName)
					.getReadMethod().invoke(bean);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static Class getRemoteActionClass(Class clazz) {
		RegistryLocation registryLocation = (RegistryLocation) clazz
				.getAnnotation(RegistryLocation.class);
		if (registryLocation != null && registryLocation
				.registryPoint() == RemoteActionPerformer.class) {
			return registryLocation.targetClass();
		}
		return null;
	}

	public static Throwable getRootCause(Throwable t) {
		int maxDepth = 100;
		while (t.getCause() != null && t.getCause() != t && maxDepth-- != 0) {
			t = t.getCause();
		}
		return t;
	}

	public static List<PropertyDescriptor>
			getSortedPropertyDescriptors(Class clazz) {
		ensureDescriptorLookup(clazz);
		List<PropertyDescriptor> result = new ArrayList<PropertyDescriptor>(
				pdLookup.asMap(clazz).allValues());
		Comparator<PropertyDescriptor> pdNameComparator = new Comparator<PropertyDescriptor>() {
			@Override
			public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};
		Collections.sort(result, pdNameComparator);
		return result;
	}

	public static String getStacktraceSlice(Thread t) {
		return getStacktraceSlice(t, 20, 0);
	}

	public static String getStacktraceSlice(Thread t, int size,
			int omitLowCount) {
		String log = "";
		StackTraceElement[] trace = t.getStackTrace();
		for (int i = omitLowCount; i < trace.length && i < size; i++) {
			log += trace[i] + "\n";
		}
		log += "\n\n";
		return log;
	}

	public static int getUniqueInt(List<Integer> ints) {
		while (true) {
			int i = (int) Math
					.max(Math.round(Math.random() * Integer.MAX_VALUE) - 1, 0);
			if (!ints.contains(i)) {
				return i;
			}
		}
	}

	public static String getUniqueNumberedString(String str,
			List<String> sibs) {
		return getUniqueNumberedString(str, sibs, '(', ')');
	}

	public static String getUniqueNumberedString(String input,
			List<String> sibs, char start, char end) {
		String str;
		for (int i = 0;; i++) {
			str = (i == 0) ? input : input + " " + start + i + end;
			if (!sibs.contains(str)) {
				return str;
			}
		}
	}

	public static boolean hasFractional(double d) {
		return Math.abs(Math.round(d) - d) > 0.0001;
	}

	public static String htmlToText(String aText) {
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = new StringCharacterIterator(
				aText);
		char character = iterator.current();
		while (character != CharacterIterator.DONE) {
			if (character == '&') {
				final StringBuilder entityBuilder = new StringBuilder();
				while (character != CharacterIterator.DONE
						&& character != ';') {
					entityBuilder.append(character);
					character = iterator.next();
				}
				String entity = entityBuilder.toString() + ";";
				if (entity.equals("&#8220;") || entity.equals("&#8221;")) {
					result.append("\"");
				} else if (entity.equals("&nbsp;")) {
					result.append(" ");
				}
			} else if (character == '<') {
				while (character != CharacterIterator.DONE
						&& character != '>') {
					character = iterator.next();
				}
			} else {
				// the char is not a special one
				// add it to the result as is
				result.append(character);
			}
			character = iterator.next();
		}
		return result.toString();
	}

	public static boolean isNullOrEmpty(String string) {
		return string == null || string.length() == 0;
	}

	public static boolean isWhitespace(char c) {
		switch (c) {
		case '\u0009':
		case '\n':
		case '\u000B':
		case '\f':
		case '\r':
		case '\u00A0':
		case ' ':
			return true;
		}
		return false;
	}

	public static boolean isWhitespace(String input) {
		return doWhitespace(input, true, '-') != null;
	}

	public static boolean isWhitespaceOrEmpty(String input) {
		return input.length() == 0 || isWhitespace(input);
	}

	public static <A extends Annotation> void iterateForPropertyWithAnnotation(
			Object object, Class<A> annotationClass,
			HasAnnotationCallback<A> hasAnnotationCallback) {
		try {
			PropertyDescriptor[] pds = Introspector
					.getBeanInfo(object.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				JvmPropertyReflector reflector = new JvmPropertyReflector(pd);
				if (reflector.getAnnotation(annotationClass) != null) {
					hasAnnotationCallback.apply(
							reflector.getAnnotation(annotationClass),
							reflector);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static List<Object> iteratorToList(Iterator i) {
		List<Object> result = new ArrayList<Object>();
		for (; i.hasNext(); result.add(i.next()))
			;
		return result;
	}

	public static String join(List<String> strings, String separator) {
		StringBuffer b = new StringBuffer();
		for (Iterator<String> itr = strings.iterator(); itr.hasNext();) {
			b.append(itr.next());
			if (itr.hasNext()) {
				b.append(separator);
			}
		}
		return b.toString();
	}

	public static List<File> listFilesRecursive(String initialPath,
			FileFilter filter) {
		return listFilesRecursive(initialPath, filter, false);
	}

	public static List<File> listFilesRecursive(String initialPath,
			FileFilter filter, boolean removeFolders) {
		return listFilesRecursive(initialPath, filter, removeFolders, null);
	}

	public static List<File> listFilesRecursive(String initialPath,
			FileFilter filter, boolean removeFolders,
			Pattern doNotCheckFolderPattern) {
		Stack<File> folders = new Stack<File>();
		List<File> results = new ArrayList<File>();
		folders.add(new File(initialPath));
		while (!folders.isEmpty()) {
			File folder = folders.pop();
			File[] files = filter == null ? folder.listFiles()
					: folder.listFiles(filter);
			for (File file : files) {
				if (doNotCheckFolderPattern == null || !doNotCheckFolderPattern
						.matcher(file.getName()).matches()) {
					if (file.isDirectory()) {
						folders.push(file);
					}
				}
				results.add(file);
			}
		}
		if (removeFolders) {
			for (Iterator<File> itr = results.iterator(); itr.hasNext();) {
				File file = itr.next();
				if (doNotCheckFolderPattern == null || !doNotCheckFolderPattern
						.matcher(file.getName()).matches()) {
					if (file.isDirectory()) {
						itr.remove();
					}
				}
			}
		}
		return results;
	}

	@SuppressWarnings("unchecked")
	public static Map listToMap(List l, Method m) throws Exception {
		Map map = new HashMap<Object, Object>();
		for (Iterator it = l.iterator(); it.hasNext();) {
			Object o = it.next();
			if (o == null) {
				continue;
			}
			Object key = m.invoke(o, CommonUtils.EMPTY_OBJECT_ARRAY);
			map.put(key, o);
		}
		return map;
	}

	public static Map<String, File> makeSingleLevelFileMap(File[] files) {
		Map<String, File> m = new HashMap<String, File>();
		for (File file : files) {
			m.put(file.getName(), file);
		}
		return m;
	}

	public static Iterable<String> matchIterable(String text, String regex) {
		return matchIterable(text, regex, 0);
	}

	public static Iterable<String> matchIterable(String text, String regex,
			int group) {
		Pattern pattern = Pattern.compile(regex);
		return new PatternIterable(pattern, text, group);
	}

	public static Stream<String> matchStream(String text, String regex) {
		return matchStream(text, regex, 0);
	}

	public static Stream<String> matchStream(String text, String regex,
			int group) {
		Iterable<String> matchIterable = matchIterable(text, regex, group);
		return StreamSupport.stream(matchIterable.spliterator(), false);
	}

	public static String normaliseEnglishTitle(String name) {
		if (name == null) {
			return null;
		}
		Matcher m = Pattern.compile("\\w+").matcher(name);
		Pattern nonTitle = Pattern.compile(
				"(?i)(?:a|an|the|and|but|o|nor|for|yet|so|as|at|by|for|in|of|on|to|from|vs|v|etc)");
		int idx2 = 0;
		StringBuilder out = new StringBuilder();
		while (m.find()) {
			String priorDelim = name.substring(idx2, m.start());
			out.append(priorDelim);
			idx2 = m.end();
			String part = m.group();
			if (m.start() == 0 || m.end() == name.length()) {
				// always capitalise
			} else {
				if (nonTitle.matcher(part).matches()) {
					part = part.toLowerCase();
				} else {
					part = CommonUtils.titleCaseKeepAcronyms(part);
				}
			}
			out.append(part);
		}
		out.append(name.substring(idx2));
		return out.toString();
	}

	public static String normalizeWhitespace(String input) {
		return doWhitespace(input, false, ' ');
	}

	public static String normalizeWhitespaceAndTrim(String input) {
		return input == null ? null : normalizeWhitespace(input).trim();
	}

	public static String normalizeWhitespaceBreaking(String input) {
		// \uE000 is 'unused (private) unicode'
		input = input.replace("\u00A0", "\uE000");
		input = normalizeWhitespace(input);
		input = input.replace("\uE000", "\u00A0");
		return input;
	}

	public static boolean notJustWhitespace(String text) {
		return SEUtilities.normalizeWhitespaceAndTrim(text).length() > 0;
	}

	public static Date oldDate(int year, int month, int dayOfMonth) {
		return toOldDate(LocalDate.of(year, month, dayOfMonth));
	}

	public static String padString(String input, int length, char padChar) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length - input.length(); i++) {
			sb.append(padChar);
		}
		sb.append(input);
		return sb.toString();
	}

	public static String removeDodgyAscii(String text) {
		if (!containsDodgyAscii(text)) {
			return text;
		}
		StringBuilder sb = new StringBuilder();
		for (int idx = 0; idx < text.length(); idx++) {
			int codePoint = (int) text.charAt(idx);
			if (codePoint >= 0x80 && codePoint <= 0x9f) {
			} else {
				sb.append((char) codePoint);
			}
		}
		return sb.toString();
	}

	public static String sanitiseFileName(String string) {
		return string.replaceAll("[\\?/<>\\|\\*:\\\\\"\\{\\}]", "_");
	}

	public static void setPropertyValue(Object bean, String propertyName,
			Object value) {
		try {
			PropertyDescriptor descriptor = getPropertyDescriptorByName(
					bean.getClass(), propertyName);
			if (descriptor == null) {
				throw new NoSuchPropertyException(propertyName);
			}
			descriptor.getWriteMethod().invoke(bean, value);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String smartQuotes(String text, boolean priorNonWhitespace) {
		if (sq_5.matcher(text).find()) {
			if (priorNonWhitespace) {
				text = "z" + text;
			}
			Matcher m = sq_6.matcher(text);
			text = m.replaceAll("\"");
			m = sq_1.matcher(text);
			text = m.replaceAll("“");
			m = sq_2.matcher(text);
			text = m.replaceAll("”");
			m = sq_3.matcher(text);
			text = m.replaceAll("‘");
			m = sq_4.matcher(text);
			text = m.replaceAll("’");
			if (priorNonWhitespace) {
				text = text.substring(1);
			}
			if (text.equals("‘. ")) {
				int debug = 3;
			}
		}
		return text;
	}

	public static Map<String, String> splitQuery(URL url)
			throws UnsupportedEncodingException {
		Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		String query = url.getQuery();
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			if (pair.isEmpty()) {
				continue;
			}
			int idx = pair.indexOf("=");
			query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
					URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		}
		return query_pairs;
	}

	public static void stringDiff(String s1, String s2) {
		for (int i = 0; i < s1.length(); i++) {
			char c = s1.charAt(i);
			char c2 = s2.charAt(i);
			if (c == c2) {
				System.out.print(c + "\t");
			} else {
				System.out.print(
						c + ": " + ((short) c) + " " + ((short) c2) + "\t");
			}
			if (i % 4 == 0) {
				System.out.println();
			}
		}
	}

	public static String stripWhitespace(String input) {
		return doWhitespace(input, false, '-');
	}

	public static void threadDump() {
		Set<Entry<Thread, StackTraceElement[]>> allStackTraces = Thread
				.getAllStackTraces().entrySet();
		for (Entry<Thread, StackTraceElement[]> entry : allStackTraces) {
			System.out.println(entry.getKey());
			StackTraceElement[] value = entry.getValue();
			for (StackTraceElement stackTraceElement : value) {
				System.out.println("\t" + stackTraceElement);
			}
		}
	}

	public static String threadDumpToString() {
		StringBuilder sb = new StringBuilder();
		Set<Entry<Thread, StackTraceElement[]>> allStackTraces = Thread
				.getAllStackTraces().entrySet();
		for (Entry<Thread, StackTraceElement[]> entry : allStackTraces) {
			sb.append(entry.getKey());
			sb.append("\n");
			StackTraceElement[] value = entry.getValue();
			for (StackTraceElement stackTraceElement : value) {
				sb.append("\t");
				sb.append(stackTraceElement);
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public static void throwFutureException(List<Future<Object>> futures)
			throws Exception {
		for (Future<Object> future : futures) {
			try {
				future.get();
			} catch (ExecutionException ee) {
				Throwable cause = ee.getCause();
				if (cause instanceof Exception) {
					Exception ne = (Exception) cause;
					throw ne;
				}
				throw new Exception(cause);
			}
		}
	}

	public static Date toOldDate(LocalDate ld) {
		return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	public static Date toOldDate(LocalDateTime ldt) {
		return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static void toStartOfDay(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	public static String trimTrailingSlash(String string) {
		if (string.endsWith("/")) {
			return string.substring(0, string.length() - 1);
		} else {
			return string;
		}
	}

	public static String usToAuDate(String s) {
		if (isNullOrEmpty(s)) {
			return s;
		}
		Pattern p = Pattern.compile("(.+?)/(.+?)/(.+?)");
		Matcher m = p.matcher(s);
		boolean b = m.matches();
		if (!m.matches()) {
			return s;
		}
		return String.format("%s/%s/%s", m.group(2), m.group(1), m.group(3));
	}

	public static IntPair yearRange(String s) {
		Matcher matcher = yearRangePattern.matcher(s);
		IntPair result = new IntPair();
		if (matcher.matches()) {
			result.i1 = Integer.parseInt(matcher.group(1));
			if (matcher.group(3) != null) {
				result.i2 = Integer.parseInt(matcher.group(3));
			} else {
				result.i2 = result.i1;
			}
		}
		return result;
	}

	private static int copyDirectory(File in, File out) throws IOException {
		int fc = 0;
		if (out.exists()) {
			if (out.isDirectory()) {
				deleteDirectory(out);
			} else {
				out.delete();
			}
		}
		out.mkdirs();
		File[] files = in.listFiles();
		for (File subIn : files) {
			File subOut = new File(
					out.getPath() + File.separator + subIn.getName());
			fc += copyFile(subIn, subOut);
		}
		return fc;
	}

	private static String doWhitespace(String input, boolean returnNullIfNonWs,
			char replace) {
		StringBuilder sb = null;
		int sct = 0;
		int nsct = 0;
		boolean escaped = false;
		boolean strip = replace == '-';
		int maxSpaceCount = strip ? 0 : 1;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch (c) {
			case '\u0009':
			case '\n':
			case '\u000B':
			case '\f':
			case '\r':
			case '\u00A0':
				nsct++;
				break;
			case ' ':
				sct++;
				break;
			default:
				nsct = 0;
				sct = 0;
				escaped = false;
				if (sb != null) {
					sb.append(c);
				}
				if (returnNullIfNonWs) {
					return null;
				}
			}
			if (!returnNullIfNonWs && sb == null
					&& (nsct > 0 || sct > maxSpaceCount)) {
				sb = new StringBuilder(input.length());
				sb.append(input.substring(0, i - (sct + nsct - 1)));
			}
			if (sb != null && !escaped && (sct > 0 || nsct > 0)) {
				if (replace != '-') {
					sb.append(' ');
				}
				escaped = true;
			}
		}
		return sb == null ? input : sb.toString();
	}

	private static void expandAll(JTree tree, TreePath parent, boolean expand) {
		// Traverse children
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			for (Enumeration e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(tree, path, expand);
			}
		}
		// Expansion or collapse must be done bottom-up
		if (expand) {
			tree.expandPath(parent);
		} else {
			tree.collapsePath(parent);
		}
	}

	protected static void ensureDescriptorLookup(Class clazz) {
		try {
			if (!pdLookup.containsKey(clazz)) {
				synchronized (pdLookup) {
					PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz)
							.getPropertyDescriptors();
					for (PropertyDescriptor pd : pds) {
						pdLookup.put(clazz, pd.getName(), pd);
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static class Bytes {
		public static int indexOf(byte[] src, byte[] toFind) {
			return indexOf(src, toFind, 0);
		}

		public static int indexOf(byte[] src, byte[] toFind, int offset) {
			int j = 0;
			for (; offset + j < src.length;) {
				if (src[offset + j] == toFind[j]) {
					j++;
					if (j == toFind.length) {
						return offset;
					}
				} else {
					j = 0;
					offset++;
				}
			}
			return -1;
		}

		public static byte[] join(List<byte[]> arrays, byte[] joinToken) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
			try {
				for (Iterator<byte[]> itr = arrays.iterator(); itr.hasNext();) {
					byte[] bs = itr.next();
					baos.write(bs);
					if (itr.hasNext()) {
						baos.write(joinToken);
					}
				}
			} catch (IOException e) {
				// never
			}
			return baos.toByteArray();
		}

		public static List<byte[]> split(byte[] src, byte[] toFind) {
			List<byte[]> result = new ArrayList<byte[]>();
			int x = 0;
			int y = 0;
			while (y != src.length) {
				y = indexOf(src, toFind, x);
				y = (y == -1) ? src.length : y;
				byte[] bytes = new byte[y - x];
				System.arraycopy(src, x, bytes, 0, y - x);
				result.add(bytes);
				x = y + toFind.length;
			}
			return result;
		}
	}

	@RegistryLocation(registryPoint = IidGenerator.class)
	public static class IidGeneratorJ2SE implements IidGenerator {
		@Override
		public String generate() {
			return generateId();
		}
	}

	public static interface ObjectFilter<T> {
		public boolean include(T obj);
	}

	public static enum OsType {
		Windows, MacOS, Unix
	}

	public static class PatternIterable implements Iterable<String> {
		private Pattern pattern;

		private String text;

		private int group;

		public PatternIterable(Pattern pattern, String text, int group) {
			this.pattern = pattern;
			this.text = text;
			this.group = group;
		}

		@Override
		public Iterator<String> iterator() {
			Matcher matcher = pattern.matcher(text);
			return new MatcherIterator(matcher);
		}

		private final class MatcherIterator implements Iterator<String> {
			private Matcher matcher;

			boolean peeked = false;

			String nextMatch;

			boolean finished = false;

			public MatcherIterator(Matcher matcher) {
				this.matcher = matcher;
			}

			@Override
			public boolean hasNext() {
				ensurePeeked();
				return !finished;
			}

			@Override
			public String next() {
				ensurePeeked();
				if (finished) {
					throw new NoSuchElementException();
				}
				peeked = false;
				return nextMatch;
			}

			private void ensurePeeked() {
				if (!peeked) {
					peeked = true;
					finished = !matcher.find();
					if (!finished) {
						nextMatch = matcher.group(group);
					}
				}
			}
		}
	}
}