package cc.alcina.framework.entity.parser.structured.node;

import java.util.List;
import java.util.stream.Collectors;

public class XmlNodes {
	public static String joinText(List<XmlNode> parts) {
		return parts.stream().map(XmlNode::textContent).map(String::trim)
				.collect(Collectors.joining(""));
	}
}
