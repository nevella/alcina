package cc.alcina.framework.entity.persistence.cache;

import cc.alcina.framework.entity.persistence.metric.StatCategory;

public class StatCategory_DomainStore extends StatCategory {
	public StatCategory_DomainStore() {
		super(null, "domain-store");
	}

	public static class StatCategory_DomainStore_End extends StatCategory {
		public StatCategory_DomainStore_End() {
			super(StatCategory_DomainStore.class, "end");
		}
	}

	public static class StatCategory_DomainStore_Prepare extends StatCategory {
		public StatCategory_DomainStore_Prepare() {
			super(StatCategory_DomainStore.class, "prepare");
		}
	}

	public static class StatCategory_DomainStore_Start extends StatCategory {
		public StatCategory_DomainStore_Start() {
			super(StatCategory_DomainStore.class, "start");
		}
	}
}