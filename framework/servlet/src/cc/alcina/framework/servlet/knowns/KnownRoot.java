package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;

public abstract class KnownRoot extends KnownNode {
	public KnownRoot(KnownsPersistence persistence, String name) {
		super(persistence, null, name);
	}

	public boolean exportRenderable(KnownRenderableNode node) {
		return true;
	}
}
