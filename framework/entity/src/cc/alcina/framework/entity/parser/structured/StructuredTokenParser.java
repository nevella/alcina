package cc.alcina.framework.entity.parser.structured;

import java.util.List;

import cc.alcina.framework.entity.parser.structured.node.XmlDoc;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;
import cc.alcina.framework.entity.parser.structured.node.XmlTokenStream;

public class StructuredTokenParser<C extends StructuredTokenParserContext> {
	private List<XmlToken> tokens;

	public XmlTokenOutput parse(Class<?> tokenClass, XmlTokenStream stream, C context) {
		this.tokens = XmlTokens.get().getTokens(tokenClass);
		XmlDoc outDoc = new XmlDoc("<root/>");
		XmlTokenOutput out = new XmlTokenOutput(outDoc);
		context.out = out;
		context.stream = stream;
		while (stream.hasNext()) {
			XmlNode node = stream.next();
			for (XmlToken token : tokens) {
				if (token.matches(context, node)) {
					XmlTokenNode outNode = new XmlTokenNode(node, token);
					token.onMatch(context, outNode);
					break;
				}
			}
		}
		context.end();
		return out;
	}
}
