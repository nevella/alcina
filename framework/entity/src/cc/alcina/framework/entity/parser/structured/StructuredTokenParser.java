package cc.alcina.framework.entity.parser.structured;

import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.parser.structured.node.XmlDoc;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;
import cc.alcina.framework.entity.parser.structured.node.XmlTokenStream;

public class StructuredTokenParser<C extends StructuredTokenParserContext> {
	public static List<XmlToken> getTokens(Class<?> tokenClass) {
		return XmlTokens.get().getTokens(tokenClass);
	}

	private List<XmlToken> tokens;

	Stack<XmlNode> openNodes;

	public XmlTokenOutput parse(Class<?> tokenClass, XmlTokenStream stream,
			C context) {
		return parse(tokenClass, stream, context, () -> true,
				getTokens(tokenClass));
	}

	public XmlTokenOutput parse(Class<?> tokenClass, XmlTokenStream stream,
			C context, Supplier<Boolean> shouldContinue,
			List<XmlToken> tokens) {
		openNodes = new Stack<>();
		this.tokens = tokens;
		XmlDoc outDoc = new XmlDoc("<root/>");
		XmlTokenOutput out = new XmlTokenOutput(outDoc);
		context.out = out;
		out.context = context;
		context.stream = stream;
		context.parser = this;
		context.start();
		int counter = 0;
		int all = (int) stream.getDoc().children.flat().count();
		while (stream.hasNext()) {
			XmlNode node = stream.next();
			closeOpenNodes(node, context);
			handleNode(node, context);
			if (!shouldContinue.get()) {
				break;
			}
			if (all > 10000 && counter++ % 1000 == 0) {
				Ax.out("%s/%s", counter, all);
			}
		}
		closeOpenNodes(null, context);
		context.end();
		return out;
	}

	private void closeOpenNodes(XmlNode node, C context) {
		while (openNodes.size() > 0) {
			XmlNode openNode = openNodes.pop();
			if (node != null && openNode.isAncestorOf(node)) {
				openNodes.push(openNode);
				break;
			} else {
				handleExitNode(openNode, context);
			}
		}
		if (node != null && node.isElement()) {
			openNodes.push(node);
		}
	}

	protected void handleExitNode(XmlNode node, C context) {
		for (XmlToken token : tokens) {
			if (token.matchesExit(context, node)) {
				XmlStructuralJoin join = new XmlStructuralJoin(node, token);
				token.onMatch(context, join);
				break;
			}
		}
	}

	protected void handleNode(XmlNode node, C context) {
		for (XmlToken token : tokens) {
			if (token.matches(context, node)) {
				XmlStructuralJoin join = new XmlStructuralJoin(node, token);
				token.onMatch(context, join);
				break;
			}
		}
	}
}
