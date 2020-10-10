package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskSwitchPostgresUrl extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		Spec spec = typedValue(Spec.class);
		DomainStore store = DomainStore.stores()
				.storeFor(spec.descriptorClassName);
		store.setConnectionUrl(spec.newUrl);
		if (store == DomainStore.writableStore()) {
			CommonPersistenceProvider.get().getCommonPersistence()
					.changeJdbcConnectionUrl(spec.newUrl);
		}
		slf4jLogger.info("Connection url changed to: {}", spec.newUrl);
	}

	public static class Spec {
		public String newUrl;

		public String descriptorClassName;
	}
}
