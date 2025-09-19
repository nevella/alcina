package cc.alcina.framework.servlet.job;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.schedule.PerformerTask;

/**
 * <p>
 * Dev-only - clear spurious pending/active jobs
 *
 */
@Bean(PropertySource.FIELDS)
public class TaskClearDevPendingJobs extends PerformerTask
		implements Task.RemotePerformable {
	@Override
	public void run() throws Exception {
		Preconditions.checkState(Ax.isTest());
		JobDomain.get().getAllJobs()
				.filter(job -> !job.provideIsComplete()
						&& job.provideTaskClass() != getClass())
				.forEach(Job::delete);
		Transaction.commit();
	}
}
