package cc.alcina.framework.entity.stat;

public class StatCategory_DomainStore extends StatCategory {
	public StatCategory_DomainStore() {
		super(StatCategory_Console.class, "domain-store");
	}

	public static class Load extends StatCategory {
		public Load() {
			super(StatCategory_DomainStore.class, "load-domainstore");
		}
	}

	public static class Prepare extends StatCategory {
		public Prepare() {
			super(StatCategory_DomainStore.class, "prepare-domainstore");
		}
	}

	public static class Start extends StatCategory {
		public Start() {
			super(StatCategory_DomainStore.class, "start");
		}
	}

	public static class StartClusterTransformListener extends StatCategory {
		public StartClusterTransformListener() {
			super(StatCategory_DomainStore.class, "cluster-tr-listener");
		}
	}

	public static class Warmup extends StatCategory {
		public Warmup() {
			super(Load.class, "warmup-start");
		}

		public static class End extends StatCategory {
			public End() {
				super(Load.class, "warmup");
			}
		}

		public static class InitialiseDescriptor extends StatCategory {
			public InitialiseDescriptor() {
				super(Warmup.class, "initialise-descriptor");
			}
		}

		public static class Loader extends StatCategory {
			public Loader() {
				super(StatCategory_DomainStore.Warmup.class, "loader-start");
			}

			public static class End extends StatCategory {
				public End() {
					super(Warmup.class, "loader");
				}
			}

			public static class JoinTables extends StatCategory {
				public JoinTables() {
					super(Loader.class, "join-tables");
				}
			}

			public static class Lookups extends StatCategory {
				public Lookups() {
					super(Loader.class, "lookups");
				}
			}

			public static class Mark extends StatCategory {
				public Mark() {
					super(Loader.class, "mark");
				}
			}

			public static class PostLoad extends StatCategory {
				public PostLoad() {
					super(Loader.class, "post-load");
				}
			}

			public static class Projections extends StatCategory {
				public Projections() {
					super(Loader.class, "projections");
				}
			}

			public static class Segment extends StatCategory {
				public Segment() {
					super(Loader.class, "segment");
				}
			}

			public static class Tables extends StatCategory {
				public Tables() {
					super(Loader.class, "tables");
				}
			}

			public static class Xrefs extends StatCategory {
				public Xrefs() {
					super(Loader.class, "xrefs");
				}
			}
		}

		public static class Mvcc extends StatCategory {
			public Mvcc() {
				super(Warmup.class, "mvcc");
			}
		}
	}
}