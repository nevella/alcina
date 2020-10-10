package cc.alcina.extras.dev.console;

import cc.alcina.framework.entity.persistence.metric.StatCategory;

public class StatCategory_All extends StatCategory {
	public StatCategory_All() {
		super(null, "console");
	}

	public static class StatCategory_InitConsole extends StatCategory {
		public StatCategory_InitConsole() {
			super(StatCategory_All.class, "init");
		}

		public static class StatCategory_InitJaxbServices extends StatCategory {
			public StatCategory_InitJaxbServices() {
				super(StatCategory_All.StatCategory_InitConsole.class,
						"init-jaxb");
			}
		}

		public static class StatCategory_InitLightweightServices
				extends StatCategory {
			public StatCategory_InitLightweightServices() {
				super(StatCategory_All.StatCategory_InitConsole.class,
						"init-lightweight-services");
			}
		}
	}

	public static class StatCategory_InitPostObjectServices
			extends StatCategory {
		public StatCategory_InitPostObjectServices() {
			super(StatCategory_All.class, "init-postobject-services");
		}
	}

	public static class StatCategory_Start extends StatCategory {
		public StatCategory_Start() {
			super(StatCategory_All.class, "console-start");
		}
	}
}