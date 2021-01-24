# Classpath ordering
'Alcina' should come before gwt-user.jar - particularly when running hosted mode (GWT serialization overrides etc)

# GWT hosted mode classloader issues
a conditional breakpoint in com.google.gwt.dev.shell.CompilingClassLoader.MultiParentClassLoader.findClass(String) is often yr friend

# also (Ax.out(resources)
com.google.gwt.dev.javac.CompilationStateBuilder.doBuildFrom(TreeLogger, CompilerContext, Set<Resource>, AdditionalTypeProviderDelegate)