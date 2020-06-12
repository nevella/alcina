package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.util.LooseContext;

/*
 * Should only be implemented by server-side domainstore
 */
public interface IDomainStore {
	String CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT = IDomainStore.class.getName()
			+ ".CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT";

	public static boolean isNonTransactionalDomain() {
		return LooseContext
				.is(IDomainStore.CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT);
	}
}
