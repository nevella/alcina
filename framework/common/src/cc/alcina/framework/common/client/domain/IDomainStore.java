package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.util.LooseContext;

public interface IDomainStore extends MemoryStatProvider {
	String CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT = IDomainStore.class.getName()
			+ ".CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT";

	public static boolean isNonTransactionalDomain() {
		return LooseContext
				.is(IDomainStore.CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT);
	}
}
