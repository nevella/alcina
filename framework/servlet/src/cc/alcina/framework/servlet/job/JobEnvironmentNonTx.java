package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.domain.LocalDomain;

/**
 * A non-mvcc environment
 * 
 * @author nick@alcina.cc
 *
 */
public class JobEnvironmentNonTx implements JobEnvironment {
	private LocalDomain localDomain;

	public JobEnvironmentNonTx(LocalDomain localDomain) {
		this.localDomain = localDomain;
	}

	@Override
	public void commit() {
		LocalDomain.Transactions.commit();
	}

	class JobDomainLocal {
	}
}
