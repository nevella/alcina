package cc.alcina.framework.servlet.job;

import cc.alcina.framework.entity.persistence.mvcc.Transaction;

/**
 * The default environment, backed by the mvcc writable DomainStore
 * 
 * @author nick@alcina.cc
 *
 */
class JobEnvironmentTx implements JobEnvironment {
	@Override
	public void commit() {
		Transaction.commit();
	}
}
