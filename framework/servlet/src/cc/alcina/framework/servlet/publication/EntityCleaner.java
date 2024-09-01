package cc.alcina.framework.servlet.publication;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;

/**
 * Convert html to unicode entities for XHTML processing
 *
 * 
 *
 */
@Registration.Singleton
public class EntityCleaner {
	public static EntityCleaner get() {
		return Registry.impl(EntityCleaner.class);
	}

	public static void main(String[] args) {
		System.err.println(EntityCleaner.get()
				.htmlToUnicodeEntities("bruce &nbsp;&euro;yep;&#8364;"));
	}

	private Map<String, String> htmlToNumericEntities = new LinkedHashMap<String, String>();

	public EntityCleaner() {
		readEntityFile();
	}

	private void append(String html, StringBuilder buf, int i, int j) {
		for (int idx = i; idx < j; idx++) {
			buf.append(html.charAt(idx));
		}
	}

	public String htmlToUnicodeEntities(String html) {
		return this.htmlToUnicodeEntities(html, false);
	}

	public String htmlToUnicodeEntities(String html, boolean strict) {
		if (html == null) {
			return null;
		}
		StringBuilder buf = new StringBuilder((int) (html.length() * 1.1));
		int length = html.length();
		int lineNumber = 1;
		int lineIdx = 0;
		for (int i = 0; i < length; i++) {
			char c = html.charAt(i);
			if (c == '\n') {
				lineNumber++;
			}
			if (c == '<') {
				// skip PIs, comments
				if (length - i < 3) {
				} else {
					if (html.charAt(i + 1) == '?') {
						int idx2 = html.indexOf("?>", i + 2);
						int idx3 = html.indexOf(">", i + 2);
						if (idx2 == -1) {
							throw new IllegalArgumentException(Ax.format(
									"Can't close processing instruction - idx %s, line %s",
									i, lineNumber));
						}
						if (idx3 != -1 && idx3 < idx2) {
							// make sure this isn't a sgml-y semi-PI ... e.g:
							// <?process>....<blah>...<?legit-pi?>
							int idx4 = html.indexOf("<?", i + 2);
							if (idx4 != -1 && idx4 < idx2) {
								throw new IllegalArgumentException(Ax.format(
										"Illegal close processing instruction - idx %s; line %s",
										i, lineNumber));
							}
						}
						append(html, buf, i, idx2 + 2);
						i = idx2 + 1;// loop will add 1
						continue;
					}
					if (html.charAt(i + 1) == '-') {
						Preconditions.checkArgument(html.charAt(i + 2) == '-');
						int idx2 = html.indexOf("-->", i + 3);
						if (idx2 == -1) {
							throw new IllegalArgumentException(
									"Can't close comment - idx " + i);
						}
						append(html, buf, i, idx2 + 3);
						i = idx2 + 2;// loop will add 1
						continue;
					}
				}
			}
			if (c != '&' || html.charAt(i + 1) == '#') {
				buf.append(c);
			} else {
				int j = html.indexOf(';', i);
				boolean found = false;
				String htmlEntity = null;
				if (j != -1) {
					htmlEntity = html.substring(i + 1, j);
					found = htmlToNumericEntities.containsKey(htmlEntity);
				}
				if (!found) {
					if (strict) {
						throw new RuntimeException(Ax.format(
								"Unknown entity - %s - index %s--%s - line %s",
								htmlEntity, i, j, lineNumber));
					}
					// unexpected & - probably unescaped, this will at least
					// produce valid xml
					j = i + 1;
					htmlEntity = "amp";
				}
				buf.append("&");
				buf.append(htmlEntity.equals("amp") ? "amp"
						: "#" + htmlToNumericEntities.get(htmlEntity));
				buf.append(';');
				i = j;
			}
		}
		return buf.toString();
	}

	public String nonAsciiToUnicodeEntities(String html) {
		StringBuffer buf = new StringBuffer(html.length());
		int length = html.length();
		for (int i = 0; i < length; i++) {
			char c = html.charAt(i);
			short s = (short) c;
			if (s <= 127) {
				buf.append(c);
			} else {
				buf.append("&#");
				buf.append(s);
				buf.append(';');
			}
		}
		return buf.toString();
	}

	private void readEntityFile() {
		try {
			String entities = Io.read().relativeTo(EntityCleaner.class)
					.resource("htmlEntities.txt").asString();
			entities = entities.replace('\u00A0', ' ');
			String regex = "(?s)<!ENTITY\\s+(\\S+)\\s+(?:CDATA)?\\s+\"&#(x)?([0-9A-F]+).+?-->";
			Pattern p = Pattern.compile(regex, Pattern.MULTILINE
					| Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(entities);
			while (m.find()) {
				String code = m.group(3);
				if (m.group(2) != null) {
					code = String.valueOf(Integer.parseInt(m.group(3), 16));
				}
				htmlToNumericEntities.put(m.group(1), code);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
