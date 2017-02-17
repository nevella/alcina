package cc.alcina.framework.entity.parser.structured;

import java.util.List;
import java.util.function.Supplier;

import cc.alcina.framework.entity.parser.structured.node.XmlDoc;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;
import cc.alcina.framework.entity.parser.structured.node.XmlTokenStream;

public class StructuredTokenParser<C extends StructuredTokenParserContext> {
	private List<XmlToken> tokens;

	public XmlTokenOutput parse(Class<?> tokenClass, XmlTokenStream stream,
			C context) {
		return parse(tokenClass, stream, context, () -> true);
	}

	public XmlTokenOutput parse(Class<?> tokenClass, XmlTokenStream stream,
			C context, Supplier<Boolean> shouldContinue) {
		this.tokens = XmlTokens.get().getTokens(tokenClass);
		XmlDoc outDoc = new XmlDoc("<root/>");
		XmlTokenOutput out = new XmlTokenOutput(outDoc);
		context.out = out;
		out.context = context;
		context.stream = stream;
		context.parser = this;
		while (stream.hasNext()) {
			XmlNode node = stream.next();
			handleNode(node, context);
			if (!shouldContinue.get()) {
				break;
			}
		}
		context.end();
		return out;
	}

	protected void handleNode(XmlNode node, C context) {
		for (XmlToken token : tokens) {
			if (token.matches(context, node)) {
				XmlStructuralJoin outNode = new XmlStructuralJoin(node, token);
				token.onMatch(context, outNode);
				break;
			}
		}
	}
}
