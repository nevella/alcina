package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.gwt.client.dirndl.StyleType;

public enum NodeRendererStyle implements StyleType {
	MISSING_NODE, MOCKUP_NODE, MOCKUP_NODE_LABEL;
	@Override
	public String prefix() {
		return "alc-dirndl-";
	}
}
