package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * A remote UI that requires a domain store transaction when executing
 */
public interface DomainUi extends RemoteUi {
	@Override
	default void onBeforeEnterContext() {
		if (isDomain()) {
			InstanceOracle.query(DomainStore.class).await();
		}
	}

	@Override
	default void onEnterIteration() {
		if (isDomain()) {
			pushUser();
			Transaction.ensureBegun();
		}
	}

	default void pushUser() {
		Permissions.pushSystemUser();
	}

	default boolean isDomain() {
		return true;
	}

	@Override
	default void onExitIteration() {
		if (isDomain()) {
			Transaction.end();
			Permissions.popContext();
		}
	}
}
