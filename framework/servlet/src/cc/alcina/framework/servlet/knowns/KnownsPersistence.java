package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.entityaccess.KnownNodePersistentDomainStore;

public interface KnownsPersistence {
	void toPersistent(KnownNode node);
	KnownRenderableNode fromPersistent(KnownNode node);
}
