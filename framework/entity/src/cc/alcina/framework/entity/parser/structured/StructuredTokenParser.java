package cc.alcina.framework.entity.parser.structured;

import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomTokenStream;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.parser.structured.StructuredTokenParserContext.PerNodeExceptionHandler;

public class StructuredTokenParser<C extends StructuredTokenParserContext> {
	public static List<XmlToken> getTokens(Class<?> tokenClass) {
		return XmlTokens.get().getTokens(tokenClass);
	}

	private List<XmlToken> tokens;

	Stack<DomNode> openNodes;

	private void closeOpenNodes(DomNode node, C context) {
		while (openNodes.size() > 0) {
			DomNode openNode = openNodes.pop();
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

	protected void handleExitNode(DomNode node, C context) {
		for (XmlToken token : tokens) {
			if (token.matchesExit(context, node)) {
				XmlStructuralJoin join = new XmlStructuralJoin(node, token);
				token.onMatch(context, join);
				break;
			}
		}
	}

	protected void handleNode(DomNode node, C context) {
		try {
			for (XmlToken token : tokens) {
				if (token.matches(context, node)) {
					XmlStructuralJoin join = new XmlStructuralJoin(node, token);
					context.logMatch(token);
					token.onMatch(context, join);
					break;
				}
			}
		} catch (RuntimeException e) {
			if (LooseContext.containsKey(
					StructuredTokenParserContext.CONTEXT_PER_NODE_EXCEPTION_HANDLER)) {
				PerNodeExceptionHandler handler = LooseContext.get(
						StructuredTokenParserContext.CONTEXT_PER_NODE_EXCEPTION_HANDLER);
				if (handler != null && !handler.isThrow(e)) {
					return;
				}
			}
			throw e;
		}
	}

	public XmlTokenOutput parse(Class<?> tokenClass, DomTokenStream stream,
			C context) {
		return parse(tokenClass, stream, context, () -> true,
				getTokens(tokenClass));
	}

	public XmlTokenOutput parse(Class<?> tokenClass, DomTokenStream stream,
			C context, Supplier<Boolean> shouldContinue,
			List<XmlToken> tokens) {
		try {
			LooseContext.push();
			openNodes = new Stack<>();
			this.tokens = tokens;
			DomDocument outDoc = DomDocument.from("<root/>");
			XmlTokenOutput out = new XmlTokenOutput(outDoc);
			LooseContext.set(DomNode.CONTEXT_DEBUG_SUPPORT, out);
			context.out = out;
			out.context = context;
			context.stream = stream;
			context.parser = this;
			context.start();
			int counter = 0;
			int all = (int) stream.getDoc().descendants().count();
			while (stream.hasNext()) {
				DomNode node = stream.next();
				closeOpenNodes(node, context);
				handleNode(node, context);
				if (!shouldContinue.get()) {
					break;
				}
				if (all > 30000 && counter++ % 5000 == 0) {
					Ax.out("%s/%s", counter, all);
				}
			}
			closeOpenNodes(null, context);
			context.end();
			return out;
		} finally {
			LooseContext.pop();
		}
	}
}
