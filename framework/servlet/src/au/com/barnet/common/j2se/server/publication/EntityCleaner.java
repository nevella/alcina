package au.com.barnet.common.j2se.server.publication;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.ResourceUtilities;
/**
 * Convert html to unicode entities for XHTML processing
 * @author nreddel@barnet.com.au
 *
 */
public class EntityCleaner {
	private Map<String, String> htmlToNumericEntities = new LinkedHashMap<String, String>();

	private EntityCleaner() {
		super();
		readEntityFile();
	}

	public static void main(String[] args) {
		System.err.println(EntityCleaner.get().htmlToUnicodeEntities(
				"bruce &nbsp;&euro;yep;&#8364;"));
	}

	public String htmlToUnicodeEntities(String html) {
		StringBuilder buf = new StringBuilder((int) (html.length() * 1.1));
		int length = html.length();
		for (int i = 0; i < length; i++) {
			char c = html.charAt(i);
			char c2 = i < length ? html.charAt(i) : 0;
			if (c2 != 0 && c == '<') {
				if (Character.isWhitespace(c2)) {
					buf.append("&lt;");
					continue;
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
					// unexpected & - probably unescaped, this will at least
					// produce valid xml
					j = i + 1;
					htmlEntity = "amp";
				}
				buf.append("&");
				buf.append(htmlEntity.equals("amp")?"amp":"#"+htmlToNumericEntities.get(htmlEntity));
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
			String entities = ResourceUtilities
					.readStreamToString(EntityCleaner.class
							.getResourceAsStream("htmlEntities.txt"));
			String regex = "<!ENTITY\\s+(\\S+)\\s+CDATA\\s+\"&#(\\d+).+?-->";
			Pattern p = Pattern.compile(regex, Pattern.MULTILINE
					| Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(entities);
			while (m.find()) {
				htmlToNumericEntities.put(m.group(1), m.group(2));
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static EntityCleaner theInstance;

	public static EntityCleaner get() {
		if (theInstance == null) {
			theInstance = new EntityCleaner();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}
}
