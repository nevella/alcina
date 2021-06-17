package cc.alcina.extras.dev.console;

import cc.alcina.framework.entity.persistence.domain.StatCategory_DomainStore;
import cc.alcina.framework.entity.persistence.metric.StatCategory;

public class StatCategory_Console extends StatCategory {
	public StatCategory_Console() {
		super(null, "console");
	}

	public static class PostDomainStore extends StatCategory {
		public PostDomainStore() {
			super(StatCategory_DomainStore.class, "console-post-start");
		}

		public static class ClassrefScanner extends StatCategory {
			public ClassrefScanner() {
				super(PostDomainStore.class, "classref-scanner");
			}
		}

		public static class End extends StatCategory {
			public End() {
				super(StatCategory_DomainStore.class, "console-post");
			}
		}

		public static class Knowns extends StatCategory {
			public Knowns() {
				super(PostDomainStore.class, "knowns");
			}
		}

		public static class SetupInstance extends StatCategory {
			public SetupInstance() {
				super(PostDomainStore.class, "setup-instance");
			}
		}
	}

	public static class StatCategory_InitConsole extends StatCategory {
		public StatCategory_InitConsole() {
			super(StatCategory_Console.class, "init");
		}

		public static class StatCategory_InitJaxbServices extends StatCategory {
			public StatCategory_InitJaxbServices() {
				super(StatCategory_Console.StatCategory_InitConsole.class,
						"init-jaxb");
			}
		}

		public static class StatCategory_InitLightweightServices
				extends StatCategory {
			public StatCategory_InitLightweightServices() {
				super(StatCategory_Console.StatCategory_InitConsole.class,
						"init-lightweight-services");
			}
		}
	}

	public static class StatCategory_InitPostObjectServices
			extends StatCategory {
		public StatCategory_InitPostObjectServices() {
			super(StatCategory_InitConsole.class, "init-postobject-services");
		}
	}

	public static class StatCategory_Start extends StatCategory {
		public StatCategory_Start() {
			super(StatCategory_Console.class, "console-start");
		}
	}
}