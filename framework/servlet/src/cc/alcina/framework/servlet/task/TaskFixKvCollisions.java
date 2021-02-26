package cc.alcina.framework.servlet.task;

import java.util.List;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparator;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.persistence.KeyValuePersistent;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskFixKvCollisions extends ServerTask<TaskFixKvCollisions> {
	@Override
	protected void performAction0(TaskFixKvCollisions task) throws Exception {
		Class<? extends KeyValuePersistent> impl = PersistentImpl
				.getImplementation(KeyValuePersistent.class);
		Multimap<String, List<KeyValuePersistent>> byKey = Domain.stream(impl)
				.collect(AlcinaCollectors
						.toKeyMultimap(KeyValuePersistent::getKey));
		byKey.entrySet().stream().forEach(e -> {
			e.getValue().stream().sorted(EntityComparator.REVERSED_INSTANCE)
					.skip(1).forEach(Entity::delete);
		});
		logger.info("Will delete {} entities",
				TransformManager.get().getTransforms().size());
		Transaction.commit();
	}
}
