package cc.alcina.framework.entity.persistence.domain.descriptor;

import java.util.Date;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.AllocationQueue.Event;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain.EventType;

/**
 * Observables emitted by the Job system. Job events are not (or should not be)
 * called in performance-critical loops, so the simpler emission pattern -
 * <code>new JobCreationEvent().publish();</code> is preferred
 * 
 */
@Bean(PropertySource.FIELDS)
public abstract class JobObservable
		implements ProcessObservable, IdOrdered<JobObservable> {
	public long id;

	public long getId() {
		return id;
	}

	public transient Job job;

	public EntityLocator locator;

	public Date date;

	// serialization constructor
	JobObservable() {
	}

	public JobObservable(Job job) {
		this.job = job;
		// locator = job.
		this.id = ProcessObservable.Id.nextId();
		this.locator = job.toLocator();
		this.date = new Date();
	}

	/**
	 * Emit on two channels - this allows global registration of say job
	 * progress trackers
	 */
	@Override
	public void publish() {
		ProcessObservers.publishUntyped(JobObservable.class, () -> this);
		ProcessObservable.super.publish();
	}

	public static class Created extends JobObservable {
		Created() {
		}

		public Created(Job job) {
			super(job);
		}
	}

	public static class Ended extends JobObservable {
		Ended() {
		}

		public Ended(Job job) {
			super(job);
		}
	}

	public static class ToProcessing extends JobObservable {
		ToProcessing() {
		}

		public ToProcessing(Job job) {
			super(job);
		}
	}

	public static class AllocationEvent extends JobObservable {
		public transient Event event;

		public EventType eventType;

		public String queueState;

		AllocationEvent() {
		}

		public AllocationEvent(AllocationQueue.Event event) {
			super(event.queue.job);
			this.event = event;
			this.eventType = event.type;
			this.queueState = event.queue.toString();
		}

		@Override
		public String toString() {
			return queueState;
		}
	}
}
