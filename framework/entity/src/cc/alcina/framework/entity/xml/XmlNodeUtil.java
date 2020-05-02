package cc.alcina.framework.entity.xml;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.xml.XmlNode;
import cc.alcina.framework.entity.MatcherIterator;

public class XmlNodeUtil {
	public static Stream<MatcherIterator> matchers(Stream<XmlNode> stream,
			String regex, int group) {
		Pattern pattern = Pattern.compile(regex);
		return stream.filter(n -> pattern.matcher(n.textContent()).find())
				.map(n -> pattern.matcher(n.textContent()))
				.map(m -> new MatcherIterator(m, group));
	}
}
