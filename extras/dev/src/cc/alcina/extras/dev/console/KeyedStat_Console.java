package cc.alcina.extras.dev.console;

import cc.alcina.framework.entity.stat.DevStats.KeyedStat;
import cc.alcina.framework.entity.stat.StatCategory_Console;
import cc.alcina.framework.entity.stat.StatCategory_Console.InitConsole;
import cc.alcina.framework.entity.stat.StatCategory_Console.InitConsole.InitJaxbServices;
import cc.alcina.framework.entity.stat.StatCategory_Console.InitConsole.InitLightweightServices;
import cc.alcina.framework.entity.stat.StatCategory_Console.InitPostObjectServices;
import cc.alcina.framework.entity.stat.StatCategory_Console.Start;
import cc.alcina.framework.entity.stat.StatCategory_DomainStore;

public class KeyedStat_Console {
	public static class ConsoleStat_StatCategory_DomainStore extends KeyedStat {
		public ConsoleStat_StatCategory_DomainStore() {
			super(StatCategory_DomainStore.Start.class,
					StatCategory_DomainStore.class);
		}

		public static class GetDescriptor extends KeyedStat {
			public GetDescriptor() {
				super(StatCategory_DomainStore.Start.class,
						StatCategory_DomainStore.GetDescriptor.class);
			}
		}

		public static class Load extends KeyedStat {
			public Load() {
				super(StatCategory_DomainStore.StartClusterTransformListener.class,
						StatCategory_DomainStore.Load.class);
			}
		}

		public static class Prepare extends KeyedStat {
			public Prepare() {
				super(StatCategory_DomainStore.GetDescriptor.class,
						StatCategory_DomainStore.Prepare.class);
			}
		}

		public static class StartClusterTransformListener extends KeyedStat {
			public StartClusterTransformListener() {
				super(StatCategory_DomainStore.Prepare.class,
						StatCategory_DomainStore.StartClusterTransformListener.class);
			}
		}

		public static class Warmup extends KeyedStat {
			public Warmup() {
				super(StatCategory_DomainStore.Warmup.class,
						StatCategory_DomainStore.Warmup.End.class);
			}

			public static class InitialiseDescriptor extends KeyedStat {
				public InitialiseDescriptor() {
					super(StatCategory_DomainStore.Warmup.class,
							StatCategory_DomainStore.Warmup.InitialiseDescriptor.class);
				}
			}

			public static class Loader extends KeyedStat {
				public Loader() {
					super(StatCategory_DomainStore.Warmup.Loader.class,
							StatCategory_DomainStore.Warmup.Loader.End.class);
				}

				public static class JoinTables extends KeyedStat {
					public JoinTables() {
						super(StatCategory_DomainStore.Warmup.Loader.Tables.class,
								StatCategory_DomainStore.Warmup.Loader.JoinTables.class);
					}
				}

				public static class Lookups extends KeyedStat {
					public Lookups() {
						super(StatCategory_DomainStore.Warmup.Loader.Segment.class,
								StatCategory_DomainStore.Warmup.Loader.Lookups.class);
					}
				}

				public static class Mark extends KeyedStat {
					public Mark() {
						super(StatCategory_DomainStore.Warmup.Loader.class,
								StatCategory_DomainStore.Warmup.Loader.Mark.class);
					}
				}

				public static class Projections extends KeyedStat {
					public Projections() {
						super(StatCategory_DomainStore.Warmup.Loader.Lookups.class,
								StatCategory_DomainStore.Warmup.Loader.Projections.class);
					}
				}

				public static class Segment extends KeyedStat {
					public Segment() {
						super(StatCategory_DomainStore.Warmup.Loader.Xrefs.class,
								StatCategory_DomainStore.Warmup.Loader.Segment.class);
					}
				}

				public static class Tables extends KeyedStat {
					public Tables() {
						super(StatCategory_DomainStore.Warmup.Loader.Mark.class,
								StatCategory_DomainStore.Warmup.Loader.Tables.class);
					}
				}

				public static class Xrefs extends KeyedStat {
					public Xrefs() {
						super(StatCategory_DomainStore.Warmup.Loader.JoinTables.class,
								StatCategory_DomainStore.Warmup.Loader.Xrefs.class);
					}
				}
			}

			public static class Mvcc extends KeyedStat {
				public Mvcc() {
					super(StatCategory_DomainStore.Warmup.InitialiseDescriptor.class,
							StatCategory_DomainStore.Warmup.Mvcc.class);
				}
			}
		}
	}

	public static class ConsoleStat_StatCategory_InitCommands
			extends KeyedStat {
		public ConsoleStat_StatCategory_InitCommands() {
			super(StatCategory_Console.InitCommands.Start.class,
					StatCategory_Console.InitCommands.class);
		}
	}

	public static class ConsoleStat_StatCategory_InitConsole extends KeyedStat {
		public ConsoleStat_StatCategory_InitConsole() {
			super(StatCategory_Console.Start.class, InitConsole.class);
		}

		public static class ConsoleStat_StatCategory_InitJaxbServices
				extends KeyedStat {
			public ConsoleStat_StatCategory_InitJaxbServices() {
				super(InitLightweightServices.class, InitJaxbServices.class);
			}
		}

		public static class ConsoleStat_StatCategory_InitLightweightServices
				extends KeyedStat {
			public ConsoleStat_StatCategory_InitLightweightServices() {
				super(Start.class, InitLightweightServices.class);
			}
		}

		public static class ConsoleStat_StatCategory_InitPostObjectServices
				extends KeyedStat {
			public ConsoleStat_StatCategory_InitPostObjectServices() {
				super(InitJaxbServices.class, InitPostObjectServices.class);
			}
		}
	}

	public static class ConsoleStatAll extends KeyedStat {
		public ConsoleStatAll() {
			super(Start.class, StatCategory_Console.class);
		}
	}

	public static class PostDomainStore extends KeyedStat {
		public PostDomainStore() {
			super(StatCategory_Console.PostDomainStore.Start.class,
					StatCategory_Console.PostDomainStore.class);
		}

		public static class Knowns extends KeyedStat {
			public Knowns() {
				super(StatCategory_Console.PostDomainStore.SetupInstance.class,
						StatCategory_Console.PostDomainStore.Knowns.class);
			}
		}

		public static class SetupInstance extends KeyedStat {
			public SetupInstance() {
				super(StatCategory_Console.PostDomainStore.class,
						StatCategory_Console.PostDomainStore.SetupInstance.class);
			}
		}
	}
}