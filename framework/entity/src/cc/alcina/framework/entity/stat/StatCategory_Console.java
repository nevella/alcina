package cc.alcina.framework.entity.stat;

public class StatCategory_Console extends StatCategory {
	public StatCategory_Console() {
		super(null, "console");
	}

	public static class InitCommands extends StatCategory {
		public InitCommands() {
			super(StatCategory_Console.class, "init-commands");
		}

		public static class Start extends StatCategory {
			public Start() {
				super(PostDomainStore.class, "init-commands-start");
			}
		}
	}

	public static class InitConsole extends StatCategory {
		public InitConsole() {
			super(StatCategory_Console.class, "init");
		}

		public static class InitJaxbServices extends StatCategory {
			public InitJaxbServices() {
				super(StatCategory_Console.InitConsole.class, "init-jaxb");
			}
		}

		public static class InitLightweightServices extends StatCategory {
			public InitLightweightServices() {
				super(StatCategory_Console.InitConsole.class,
						"init-lightweight-services");
			}
		}
	}

	public static class InitPostObjectServices extends StatCategory {
		public InitPostObjectServices() {
			super(InitConsole.class, "init-postobject-services");
		}
	}

	public static class PostDomainStore extends StatCategory {
		public PostDomainStore() {
			super(StatCategory_Console.class, "console-post");
		}

		public static class Knowns extends StatCategory {
			public Knowns() {
				super(PostDomainStore.class, "knowns");
			}
		}

		public static class PreInstance extends StatCategory {
			public PreInstance() {
				super(PostDomainStore.class, "pre-instance");
			}
		}

		public static class SetupInstance extends StatCategory {
			public SetupInstance() {
				super(PostDomainStore.class, "setup-instance");
			}
		}

		public static class Start extends StatCategory {
			public Start() {
				super(PostDomainStore.class, "console-post-start");
			}
		}
	}

	public static class Start extends StatCategory {
		public Start() {
			super(StatCategory_Console.class, "console-start");
		}
	}
}