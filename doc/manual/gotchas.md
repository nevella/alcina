# Classpath ordering
'Alcina' should come before gwt-user.jar - particularly when running hosted mode (GWT serialization overrides etc)

# GWT hosted mode classloader issues
a conditional breakpoint in com.google.gwt.dev.shell.CompilingClassLoader.MultiParentClassLoader.findClass(String) is often yr friend

# also (Ax.out(resources)
com.google.gwt.dev.javac.CompilationStateBuilder.doBuildFrom(TreeLogger, CompilerContext, Set<Resource>, AdditionalTypeProviderDelegate)

# where is my cache?
PersistentUnitCacheDir line 355

# the dangers of alcina-entity.jar
gwt serialization issues? clean & rebuild that jar

# debugging serialization incompatibility
Look in /tmp/rpclog == compare first the xx fromBrowser & xx fromServer files (to see how hosted & production mode differ)
Then compare xxx.short (a little more involved)