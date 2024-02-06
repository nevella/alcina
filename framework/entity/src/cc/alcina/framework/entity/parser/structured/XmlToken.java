package cc.alcina.framework.entity.parser.structured;

import cc.alcina.framework.common.client.dom.DomNode;

public abstract class XmlToken<C extends StructuredTokenParserContext> {
	protected String name;

	public XmlToken(String name) {
		this.name = name;
		XmlTokens.get().register(getCategory(), this);
	}

	protected abstract Class getCategory();

	public XmlTokenContext getInputContext(XmlStructuralJoin node) {
		XmlTokenContext tokenContext = new XmlTokenContext();
		tokenContext.properties = node.sourceNode.attributes();
		return tokenContext;
	}

	public XmlTokenContext getOutputContext(XmlStructuralJoin node) {
		return XmlTokenContext.EMPTY;
	}

	protected Class getSubCategory() {
		return getCategory();
	}

	public boolean ignoreable() {
		return false;
	}

	public abstract boolean matches(C context, DomNode node);

	public boolean matchesExit(C context, DomNode node) {
		return false;
	}

	public XmlToken matchOrderBefore() {
		return null;
	}

	public void onMatch(C context, XmlStructuralJoin join) {
		context.wasMatched(join);
		onMatchPreWrappers(context, join);
	}

	protected void onMatchPostWrappers(C context, XmlStructuralJoin join) {
	}

	protected abstract void onMatchPreWrappers(C context,
			XmlStructuralJoin join);

	public String textContent(DomNode sourceNode) {
		return null;
	}

	@Override
	public String toString() {
		return "XmlToken: " + name;
	}
}
