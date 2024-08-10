# Classpath ordering

'Alcina' should come before gwt-user.jar - particularly when running hosted mode (GWT serialization overrides etc)

# GWT hosted mode classloader issues

a conditional breakpoint in com.google.gwt.dev.shell.CompilingClassLoader.MultiParentClassLoader.findClass(String) is often yr friend
or if (emmaAvailable) { [[line 489 - findClassBytes]] }

# also (Ax.out(resources)

com.google.gwt.dev.javac.CompilationStateBuilder.doBuildFrom(TreeLogger, CompilerContext, Set<Resource>, AdditionalTypeProviderDelegate)

# where is my cache?

PersistentUnitCacheDir line 355

# gwt compilation error logging?

CompilationStateBuilder l 373
(and set suppressErrors=false)

# the dangers of alcina-entity.jar

gwt serialization issues? clean & rebuild that jar

# debugging serialization incompatibility

Look in /tmp/rpclog [/var/local/gwt/rpclog/] == compare first the xx fromBrowser & xx fromServer files (to see how hosted & production mode differ)
Then compare xxx.short (a little more involved)

# debug a large string (by writing to fs)

java.nio.file.Files.write(java.nio.file.Path.of("/tmp/tmp.txt"), result.getBytes());
java.nio.file.Files.write(java.nio.file.Path.of("/g/alcina/tmp/t0.html"), token.localMarkup.getBytes());

# where's that md5 get generated

SerializationUtils.getSerializationSignature - note that first thing to try is clear gwt unit cache (above)

# where are all the alcina caches n how do I clear them

FIXME console - use registry (probably) to model caches + deletion

# tracking event binding

on the actual dom, events are bound at com.google.gwt.user.client.DOM.sinkEvents(Element elem, int eventBits)
and fired at com.google.gwt.user.client.DOM.dispatchEventImpl(Event event, Element elem, EventListener listener)

# debugging devmode exceptions
