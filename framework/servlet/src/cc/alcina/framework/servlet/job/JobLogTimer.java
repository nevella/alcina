package cc.alcina.framework.servlet.job;

import java.util.Optional;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.QueryResult;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.gwt.client.util.EventCollator;

@Registration.Singleton
/*
 * writes long-running job progress messages to stdout, optional
 */
public class JobLogTimer implements DomainTransformPersistenceListener {
	public static JobLogTimer get() {
		return Registry.impl(JobLogTimer.class);
	}

	private EventCollator<Job> timer;

	public JobLogTimer() {
	}

	public void init() {
		DomainStore.stores().writableStore().getPersistenceEvents()
				.addDomainTransformPersistenceListener(this);
		int delay = 2000;
		this.timer = new EventCollator<Job>(delay, () -> {
			MethodContext.instance().withWrappingTransaction().run(() -> {
				Job job = timer.getLastObject();
				if (job.provideIsActive()) {
					Ax.out("[Job progress :: %s] - %s", job,
							job.getStatusMessage());
				}
			});
		}).withMaxDelayFromFirstEvent(delay);
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
				Optional<QueryResult> firstEntity = collation
						.query(jobImplClass).stream()
						.filter(QueryResult::hasNoDeleteTransform).findFirst();
				firstEntity.ifPresent(qr -> {
					Job job = qr.getEntity();
					timer.triggerEventOccurred(job);
				});
			}
		}
		}
	}
}