package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;

@Registration.Singleton
/*
 * writes long-running job progress messages to stdout, optional
 */
public class JobLogTimer implements DomainTransformPersistenceListener {
	public static JobLogTimer get() {
		return Registry.impl(JobLogTimer.class);
	}

	private AtEndOfEventSeriesTimer<Job> timer;

	public JobLogTimer() {
	}

	public void init() {
		DomainStore.stores().writableStore().getPersistenceEvents()
				.addDomainTransformPersistenceListener(this);
		int delay = 2000;
		this.timer = new AtEndOfEventSeriesTimer<Job>(delay, () -> {
			MethodContext.instance().withWrappingTransaction().run(() -> {
				Job job = timer.getLastObject();
				if (!job.provideIsComplete()) {
					Ax.out("[Job progress :: %s] - %s", job,
							job.getStatusMessage());
				}
			});
		}).maxDelayFromFirstAction(delay);
	}

	@Override
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent event) {
		switch (event.getPersistenceEventType()) {
		case COMMIT_OK: {
			TransformCollation collation = event.getPostProcessCollation();
			Class<? extends Job> jobImplClass = PersistentImpl
					.getImplementation(Job.class);
			if (collation.has(jobImplClass)) {
				Job job = collation.query(jobImplClass).stream().findFirst()
						.get().<Job> getEntity();
				timer.triggerEventOccurred(job);
			}
		}
		}
	}
}