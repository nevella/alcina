package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;

public interface KnownsPersistence {
	void toPersistent(KnownNode node);

	KnownRenderableNode fromPersistent(KnownNode node);
}
