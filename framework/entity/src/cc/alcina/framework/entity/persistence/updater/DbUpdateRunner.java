package cc.alcina.framework.entity.persistence.updater;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.LocalDbPropertyBase;

public class DbUpdateRunner {
	public void run(EntityManager em, boolean preCacheWarmup) throws Exception {
		List<DbUpdater> updaters = Registry.query(DbUpdater.class)
				.implementations().sorted().collect(Collectors.toList());
		if (updaters.isEmpty()) {
			return;
		}
		CommonPersistenceLocal cp = CommonPersistenceProvider.get()
				.getCommonPersistence();
		LocalDbPropertyBase dbProperty = LocalDbPropertyBase
				.getLocalDbPropertyObject(
						LocalDbPropertyBase.DB_UPDATE_VERSION);
		Integer currentUpdateNumber = dbProperty.getPropertyValue() == null ? 0
				: Integer.parseInt(dbProperty.getPropertyValue());
		for (DbUpdater dbUpdater : updaters) {
			if (dbUpdater.getUpdateNumber() > currentUpdateNumber) {
				if (dbUpdater.runPreCache() ^ preCacheWarmup) {
					continue;
				}
				if (!dbUpdater.runPreCache()) {
					throw new RuntimeException(
							"'run pre-cache==false' not supported because of update series collisions. Run as a servlet layer updater");
				}
				if (em == null && !dbUpdater.allowNullEntityManager()) {
					continue;
				}
				System.out.println(Ax.format("Running update %s: %s",
						dbUpdater.getUpdateNumber(),
						dbUpdater.getClass().getSimpleName()));
				dbUpdater.run(em);
				if (em != null) {
					dbProperty.setPropertyValue(
							dbUpdater.getUpdateNumber().toString());
					em.flush();
				} else {
					LocalDbPropertyBase.getOrSetLocalDbProperty(
							LocalDbPropertyBase.DB_UPDATE_VERSION,
							dbUpdater.getUpdateNumber().toString(), false);
				}
			}
		}
	}
}
