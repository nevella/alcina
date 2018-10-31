package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;

public abstract class KnownRoot extends KnownNode {
	public KnownRoot(String name) {
		super(null, name);
	}

	public boolean exportRenderable(KnownRenderableNode node) {
		return true;
	}
}
