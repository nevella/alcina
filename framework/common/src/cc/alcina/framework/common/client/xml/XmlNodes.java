package cc.alcina.framework.common.client.xml;

import java.util.List;
import java.util.stream.Collectors;

public class XmlNodes {
	public static String joinText(List<XmlNode> parts) {
		return parts.stream().map(XmlNode::textContent)
				.collect(Collectors.joining(""));
	}
}
