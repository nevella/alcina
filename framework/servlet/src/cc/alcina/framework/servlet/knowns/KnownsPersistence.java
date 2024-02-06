package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;

public interface KnownsPersistence {
	KnownRenderableNode fromPersistent(KnownNode node);

	void toPersistent(KnownNode node);
}
