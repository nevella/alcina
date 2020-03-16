package cc.alcina.framework.common.client.sync;

import com.google.gwt.core.shared.GWT;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;

public interface AbstractTypedLocalDomainLocatable<T extends TypedLocalDomainLocatable>
		extends TypedLocalDomainLocatable<T> {
	public static final String CONTEXT_HINT_ALLOW_CACHED_FIND = AbstractTypedLocalDomainLocatable.class
			.getName() + ".CONTEXT_HINT_ALLOW_CACHED_FIND";

	default T createOrReturnLocal() {
		return createOrReturnLocal(false);
	}

	default T createOrReturnLocal(boolean allowCached) {
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

	default void deleteLocalEquivalent() {
		if (GWT.isClient() && this instanceof Entity) {
			TransformManager.get().deleteObject((Entity) this, true);
		} else {
			Registry.impl(TypedLocalDomainPersistence.class, getClass())
					.deleteLocalEquivalent(this);
		}
	}

	default T ensureLocalEquivalent() {
		return (T) Registry.impl(TypedLocalDomainPersistence.class, getClass())
				.ensureLocalEquivalent(this);
	}

	default T findLocalEquivalent() {
		return (T) Registry.impl(TypedLocalDomainPersistence.class, getClass())
				.findLocalEquivalent(this);
	}

	default T localEquivalentOrSelf() {
		try {
			LooseContext.pushWithKey(CONTEXT_HINT_ALLOW_CACHED_FIND, true);
			T local = findLocalEquivalent();
			return (T) (local != null ? local : this);
		} finally {
			LooseContext.pop();
		}
	}

	default T updateLocalEquivalent() {
		try {
			LooseContext.pushWithKey(CONTEXT_HINT_ALLOW_CACHED_FIND, true);
			Registry.impl(TypedLocalDomainPersistence.class, getClass())
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
			Registry.impl(TypedLocalDomainPersistence.class, getClass())
					.adjustUpdateContext();
			return ensureLocalEquivalent();
		} finally {
			LooseContext.pop();
		}
	}
}
