package cc.alcina.framework.common.client.sync;

import com.google.gwt.core.shared.GWT;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
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

	public T localEquivalentOrSelf() {
		try {
			LooseContext.pushWithKey(CONTEXT_HINT_ALLOW_CACHED_FIND, true);
			T local = findLocalEquivalent();
			return (T) (local != null ? local : this);
		} finally {
			LooseContext.pop();
		}
	}

	public T createOrReturnLocal() {
		return createOrReturnLocal(false);
	}

	public T createOrReturnLocal(boolean allowCached) {
		if (allowCached) {
			try {
				LooseContext.pushWithKey(CONTEXT_HINT_ALLOW_CACHED_FIND, true);
				T local = findLocalEquivalent();
				if (local != null) {
					return local;
				} else {
					return ensureLocalEquivalent();
				}
			} finally {
				LooseContext.pop();
			}
		} else {
			T local = findLocalEquivalent();
			if (local != null) {
				return local;
			} else {
				return ensureLocalEquivalent();
			}
		}
	}

	public T updateLocalEquivalent() {
		try {
			LooseContext.pushWithKey(CONTEXT_HINT_ALLOW_CACHED_FIND, true);
			Registry.impl(LocalDomainPersistence.class, getClass())
					.adjustUpdateContext();
			T local = findLocalEquivalent();
			if (local != null && local.equivalentTo(this)) {
				if (local == this) {
					throw new RuntimeException("comparing to identical object");
				}
				return null;
			}
		} finally {
			LooseContext.pop();
		}
		try {
			LooseContext.push();
			Registry.impl(LocalDomainPersistence.class, getClass())
					.adjustUpdateContext();
			return ensureLocalEquivalent();
		} finally {
			LooseContext.pop();
		}
	}

	public void deleteLocalEquivalent() {
		if (GWT.isClient() && this instanceof HasIdAndLocalId) {
			TransformManager.get().deleteObject((HasIdAndLocalId) this, true);
		} else {
			Registry.impl(LocalDomainPersistence.class, getClass())
					.deleteLocalEquivalent(this);
		}
	}
}
