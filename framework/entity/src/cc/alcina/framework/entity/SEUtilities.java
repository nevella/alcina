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
import java.beans.IntrospectionException;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.reflection.HasAnnotationCallback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.util.JvmPropertyReflector;

/**
 * @author nick@alcina.cc
 * 
 */
public class SEUtilities {
	public static int idCounter = 1;

	@SuppressWarnings("unchecked")
	public static <C> C collectionItemOfClass(Collection coll, Class<C> clazz) {
		for (Object object : coll) {
			if (object.getClass() == clazz) {
				return (C) object;
			}
		}
		return null;
	}

	public static String consoleReadline(String prompt) {
		System.out.println(prompt);
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

	@SuppressWarnings("unchecked")
	public static <T> T getOrCreate(Collection<T> existing,
			String propertyName, String propertyValue, Class itemClass)
			throws Exception {
		PropertyDescriptor descriptor = descriptorByName(itemClass,
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

	public static void dumpStringBytes(String s) {
		try {
			dumpBytes(s.getBytes("UTF-16"), 8);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
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
				bd.append(in ? CommonUtils.padStringLeft(
						Integer.toHexString(b), 2, '0') : "  ");
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

	private static Pattern yearRangePattern = Pattern
			.compile("(\\d{4})(-(\\d{4}))?");

	public static String normalizeWhitespace(String input) {
		return doWhitespace(input, false, ' ');
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

	public static String normalizeWhitespaceAndTrim(String input) {
		return normalizeWhitespace(input).trim();
	}

	public static String stripWhitespace(String input) {
		return doWhitespace(input, false, '-');
	}

	public static boolean isWhitespace(String input) {
		return doWhitespace(input, true, '-') != null;
	}

	public static boolean isWhitespaceOrEmpty(String input) {
		return input.length() == 0 || isWhitespace(input);
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

	private static UnsortedMultikeyMap<PropertyDescriptor> pdLookup = new UnsortedMultikeyMap<PropertyDescriptor>(
			2);

	public static PropertyDescriptor descriptorByName(Class clazz,
			String propertyName) throws IntrospectionException {
		if (pdLookup.containsKey(clazz, propertyName)) {
			PropertyDescriptor cached = pdLookup.get(clazz, propertyName);
			return cached;
		}
		PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz)
				.getPropertyDescriptors();
		PropertyDescriptor result = null;
		for (PropertyDescriptor pd : pds) {
			if (pd.getName().equals(propertyName)) {
				result = pd;
				break;
			}
		}
		pdLookup.put(clazz, propertyName, result);
		return result;
	}

	public static String dumpProperties(Properties p) {
		StringWriter sw = new StringWriter();
		sw.write("--listing properties--\n");
		for (Enumeration<?> names = p.propertyNames(); names.hasMoreElements();) {
			String name = (String) names.nextElement();
			sw.write(name + "=" + p.getProperty(name) + "\n");
		}
		return sw.toString();
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
			return URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			// ahhh - doesn't happen much
			throw new WrappedRuntimeException(e);
		}
	}

	public static String decUtf8(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (Exception e) {
			// ahhh - doesn't happen much
			throw new WrappedRuntimeException(e);
		}
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

	public static File getDesktopFolder() {
		switch (getOsType()) {
		case Windows:
		case MacOS:
		case Unix:
			File file = new File(System.getProperty("user.home")
					+ File.separator + "Desktop");
			return (file.exists()) ? file : new File(
					System.getProperty("user.home"));
		default:
			return null;
		}
	}

	public static String getNameFromPath(String path) {
		path = path.replace('\\', '/');
		return (path.contains("/")) ? path.substring(path.lastIndexOf("/") + 1)
				: path;
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

	public static Object getPropertyValue(Object bean, String propertyName) {
		try {
			return descriptorByName(bean.getClass(), propertyName)
					.getReadMethod().invoke(bean);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static Throwable getRootCause(Throwable t) {
		int maxDepth = 100;
		while (t.getCause() != null && t.getCause() != t && maxDepth-- != 0) {
			t = t.getCause();
		}
		return t;
	}

	public static int getUniqueInt(List<Integer> ints) {
		while (true) {
			int i = (int) Math.max(
					Math.round(Math.random() * Integer.MAX_VALUE) - 1, 0);
			if (!ints.contains(i)) {
				return i;
			}
		}
	}

	public static String getUniqueNumberedString(String str, List<String> sibs) {
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

	public static String htmlToText(String aText) {
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = new StringCharacterIterator(
				aText);
		char character = iterator.current();
		while (character != CharacterIterator.DONE) {
			if (character == '&') {
				final StringBuilder entityBuilder = new StringBuilder();
				while (character != CharacterIterator.DONE && character != ';') {
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
				while (character != CharacterIterator.DONE && character != '>') {
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

	public static boolean localTestMode() {
		return Boolean
				.valueOf(System.getProperty("alcina.local.test", "false"));
	}

	public static Map<String, File> makeSingleLevelFileMap(File[] files) {
		Map<String, File> m = new HashMap<String, File>();
		for (File file : files) {
			m.put(file.getName(), file);
		}
		return m;
	}

	public static String padString(String input, int length, char padChar) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length - input.length(); i++) {
			sb.append(padChar);
		}
		sb.append(input);
		return sb.toString();
	}

	public static void setPropertyValue(Object bean, String propertyName,
			Object value) {
		try {
			descriptorByName(bean.getClass(), propertyName).getWriteMethod()
					.invoke(bean, value);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
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

	public static String getAccessorName(Field field) {
		return (field.getType() == boolean.class ? "is" : "get")
				+ CommonUtils.capitaliseFirst(field.getName());
	}

	public static int copyFile(File in, File out) throws IOException {
		if (in.isDirectory()) {
			return copyDirectory(in, out);
		}
		if (!out.exists()) {
			out.getParentFile().mkdirs();
			out.createNewFile();
		} else {
			if (out.lastModified() >= in.lastModified()) {
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
			File subOut = new File(out.getPath() + File.separator
					+ subIn.getName());
			fc += copyFile(subIn, subOut);
		}
		return fc;
	}

	public static String getHomeDir() {
		return (System.getenv("USERPROFILE") != null) ? System
				.getenv("USERPROFILE") : System.getProperty("user.home");
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

	public static interface ObjectFilter<T> {
		public boolean include(T obj);
	}

	public static enum OsType {
		Windows, MacOS, Unix
	}

	public static void stringDiff(String s1, String s2) {
		for (int i = 0; i < s1.length(); i++) {
			char c = s1.charAt(i);
			char c2 = s2.charAt(i);
			if (c == c2) {
				System.out.print(c + "\t");
			} else {
				System.out.print(c + ": " + ((short) c) + " " + ((short) c2)
						+ "\t");
			}
			if (i % 4 == 0) {
				System.out.println();
			}
		}
	}

	public static void dump(List list) {
		System.out.println("List:");
		for (Object object : list) {
			System.out.println("\t- " + object);
		}
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

	public static String getFullExceptionMessage(Throwable t) {
		StringWriter sw = new StringWriter();
		sw.write(t.getMessage() + "\n");
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	public static List<File> listFilesRecursive(String initialPath,
			FileFilter filter) {
		return listFilesRecursive(initialPath, filter, false);
	}

	public static List<File> listFilesRecursive(String initialPath,
			FileFilter filter, boolean removeFolders) {
		Stack<File> folders = new Stack<File>();
		List<File> results = new ArrayList<File>();
		folders.add(new File(initialPath));
		while (!folders.isEmpty()) {
			File folder = folders.pop();
			File[] files = filter == null ? folder.listFiles() : folder
					.listFiles(filter);
			for (File file : files) {
				if (file.isDirectory()) {
					folders.push(file);
				}
				results.add(file);
			}
		}
		if (removeFolders) {
			CollectionFilters.filterInPlace(results,
					new CollectionFilter<File>() {
						@Override
						public boolean allow(File o) {
							return !o.isDirectory();
						}
					});
		}
		return results;
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

	public static File getChildFile(File folder, String path) {
		return new File(String.format("%s/%s", folder.getPath(), path));
	}

	public static void appShutdown() {
		pdLookup = null;
	}

	public static String sanitiseFileName(String string) {
		return string.replaceAll("[\\?/<>\\|\\*:\\\\\"\\{\\}]", "_");
	}

	public static <A extends Annotation> void iterateForPropertyWithAnnotation(
			Object object, Class<A> annotationClass,
			HasAnnotationCallback<A> hasAnnotationCallback) {
		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(
					object.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				JvmPropertyReflector reflector = new JvmPropertyReflector(pd);
				if (reflector.getAnnotation(annotationClass) != null) {
					hasAnnotationCallback
							.apply(reflector.getAnnotation(annotationClass),
									reflector);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}