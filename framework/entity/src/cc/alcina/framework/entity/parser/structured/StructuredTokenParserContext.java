package cc.alcina.framework.entity.parser.structured;

import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;
import cc.alcina.framework.entity.parser.structured.node.XmlTokenStream;

public class StructuredTokenParserContext {
	public XmlTokenOutput out;

	public XmlTokenStream stream;

	public boolean had(XmlToken token) {
		return matched.containsKey(token);
	}

	public Multimap<XmlToken, List<XmlNode>> matched = new Multimap<>();

	public void wasMatched(XmlTokenNode outNode) {
		matched.add(outNode.token, outNode.sourceNode);
	}

	StringMap properties = new StringMap();

	public void propertyDelta(String key, boolean add) {
		properties.setBooleanOrRemove(key, add);
	}

	public void skipChildren() {
		stream.skipChildren();
	}

	int lastDepthOut = 0;

	int initialDepthOut = -1;

	int lastDepthIn = 0;

	int initialDepthIn = -1;

	public void end() {
	}

	public void log(XmlTokenNode outNode) {
		XmlNode targetNode = outNode.targetNode;
		XmlNode sourceNode = outNode.sourceNode;
		int depthOut = lastDepthOut + 1;
		if (targetNode != null) {
			depthOut = targetNode.depth();
			if (initialDepthOut == -1) {
				initialDepthOut = depthOut;
			}
			lastDepthOut = depthOut;
		}
		String depthOutSpacer = CommonUtils.padStringLeft("",
				(depthOut - initialDepthOut) * 2, " ");
		int depthIn = lastDepthIn + 1;
		if (targetNode != null) {
			depthIn = targetNode.depth();
			if (initialDepthIn == -1) {
				initialDepthIn = depthIn;
			}
			lastDepthIn = depthIn;
		}
		String depthInSpacer = CommonUtils.padStringLeft("",
				(depthIn - initialDepthIn) * 2, " ");
		String outStr = targetNode == null ? "(no output)"
				: targetNode.debug().shortRepresentation();
		String inStr = sourceNode == null ? "(no input)"
				: sourceNode.debug().shortRepresentation();
		String match = outNode.token.name;
		System.out.format("%-30s%-30s%s\n", depthInSpacer + inStr,
				depthOutSpacer + outStr, match);
	}
}
