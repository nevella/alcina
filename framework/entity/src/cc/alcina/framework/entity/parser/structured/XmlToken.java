package cc.alcina.framework.entity.parser.structured;

import cc.alcina.framework.entity.parser.structured.node.XmlNode;

public abstract class XmlToken<C extends StructuredTokenParserContext> {
	protected String name;

	protected abstract Class getCategory();

	protected Class getSubCategory() {
		return getCategory();
	}

	public XmlToken(String name) {
		this.name = name;
		XmlTokens.get().register(getCategory(), this);
	}

	public abstract boolean matches(C context, XmlNode node);

	public void onMatch(C context, XmlStructuralJoin outNode) {
		context.wasMatched(outNode);
		onMatch0(context, outNode);
	}

	protected abstract void onMatch0(C context, XmlStructuralJoin outNode);

	@Override
	public String toString() {
		return "XmlToken: " + name;
	}

	public boolean ignoreable() {
		return false;
	}

	public String textContent(XmlNode sourceNode) {
		return null;
	}

	public XmlTokenContext getOutputContext(XmlStructuralJoin node) {
		return XmlTokenContext.EMPTY;
	}

	public XmlTokenContext getInputContext(XmlStructuralJoin node) {
		XmlTokenContext tokenContext = new XmlTokenContext();
		tokenContext.properties = node.sourceNode.attributes();
		return tokenContext;
	}
}
