package cc.alcina.framework.servlet.local;

import cc.alcina.framework.servlet.job.JobEnvironmentNonTx;
import cc.alcina.framework.servlet.job.JobRegistry;

/**
 * Support for the jobs system outside mvcc
 * 
 * 
 *
 */
public class LocalDomainJobs {
	public void init(LocalDomainStore localDomainStore) {
		JobEnvironmentNonTx jobEnvironment = new JobEnvironmentNonTx();
		JobRegistry.get().setEnvironment(jobEnvironment);
		JobRegistry.get().init();
	}
}
