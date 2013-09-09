package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;

public abstract class AbstractLocalDomainLocatable<T extends LocalDomainLocatable>
		implements LocalDomainLocatable<T> {
	public static final String CONTEXT_HINT_ALLOW_CACHED_FIND = AbstractLocalDomainLocatable.class
			.getName() + ".CONTEXT_HINT_ALLOW_CACHED_FIND";

	public T findLocalEquivalent() {
		return (T) Registry.impl(LocalDomainPersistence.class, getClass())
				.findLocalEquivalent(this);
	}

	public T ensureLocalEquivalent() {
		return (T) Registry.impl(LocalDomainPersistence.class, getClass())
				.ensureLocalEquivalent(this);
	}

	public T createOrReturnLocal() {
		T local = findLocalEquivalent();
		if (local != null) {
			return local;
		} else {
			return ensureLocalEquivalent();
		}
	}

	public T updateLocalEquivalent() {
		try {
			LooseContext.pushWithKey(CONTEXT_HINT_ALLOW_CACHED_FIND, true);
			T local = findLocalEquivalent();
			if (local != null && local.equivalentTo(this)) {
				return null;
			}
		} finally {
			LooseContext.pop();
		}
		return ensureLocalEquivalent();
	}

	public void deleteLocalEquivalent() {
		Registry.impl(LocalDomainPersistence.class, getClass())
				.deleteLocalEquivalent(this);
	}
}
