package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobState;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

@RegistryLocation(registryPoint = TowardsAMoreDesirableSituation.class, implementationType = ImplementationType.SINGLETON)
public class TowardsAMoreDesirableSituation {
	public static TowardsAMoreDesirableSituation get() {
		return Registry.impl(TowardsAMoreDesirableSituation.class);
	}

	private List<Job> activeJobs = new ArrayList<>();

	Logger logger = LoggerFactory.getLogger(getClass());

	public synchronized void tend() {
		if (!ResourceUtilities.is("enabled")) {
			return;
		}
		activeJobs.removeIf(Job::provideIsSequenceComplete);
		boolean delta = false;
		while (activeJobs.size() < JobRegistry.get().jobExecutors
				.getMaxConsistencyJobCount()) {
			if (JobDomain.get().getFutureConsistencyJobs().findFirst()
					.isPresent()) {
				JobRegistry.get()
						.withJobMetadataLock(getClass().getSimpleName(), () -> {
							Optional<Job> next = JobDomain.get()
									.getFutureConsistencyJobs().findFirst();
							if (next.isPresent()) {
								Job job = next.get();
								job.setPerformer(ClientInstance.self());
								job.setState(JobState.PENDING);
								activeJobs.add(job);
								Transaction.commit();
								logger.info(
										"TowardsAMoreDesirableSituation - consitency-to-pending - {} - {} remaining",
										job,
										JobDomain.get()
												.getFutureConsistencyJobs()
												.count());
							}
						});
			} else {
				break;
			}
		}
	}
}
