package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * A remote UI that requires a domain store transaction when executing
 */
public interface DomainUi extends RemoteUi {
	@Override
	default void onBeforeEnterContext() {
		InstanceOracle.query(DomainStore.class).await();
	}

	@Override
	default void onEnterIteration() {
		PermissionsManager.get().pushSystemUser();
		Transaction.ensureBegun();
	}

	@Override
	default void onExitIteration() {
		Transaction.end();
		PermissionsManager.get().popSystemUser();
	}
}
