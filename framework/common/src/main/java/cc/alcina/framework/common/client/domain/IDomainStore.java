package cc.alcina.framework.common.client.domain;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;

public interface IDomainStore extends MemoryStatProvider {
	String CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT = IDomainStore.class.getName()
			+ ".CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT";

	public static boolean isNonTransactionalDomain() {
		return LooseContext
				.is(IDomainStore.CONTEXT_NON_TRANSACTIONAL_DOMAIN_INIT)
				|| State.nonTransactional;
	}

	public static class State {
		public static boolean nonTransactional;
	}
}
