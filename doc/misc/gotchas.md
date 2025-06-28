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

# cascading history changes

it breaks stuff! always history.replace for any cascaded changes

# hash collisions for ranges

[1,3] shouldn't be hash(1)^hash(3) - you get collisions with the delta. instead, hash(1)+hash(3)+hash(1^3)

# ui - emit cleanup before changing global state

see cc.alcina.framework.servlet.component.traversal.LayerFilterEditor.FilterSuggestor.onSelectionChanged(SelectionChanged event)

# dirndl - event reemission of the same type

if an event handler reemits an event of the same type, but with (say) more data, make sure to check reemit:

```
if (event.getModel()!=null) {
	event.bubble();
	return;
}
event.reemitAs(this, ModelEvent.class,
some data);
```

### Local copies of t/unsafe fields

Always, always get a local copy of a field if it's not thread-safe. The following caused me a few hours of head-scratching:

```
//cc.alcina.framework.entity.persistence.mvcc.Transactions.TransactionsStats.getTimeInVacuum()

public long getTimeInVacuum() {
	return vacuum.getVacuumStarted() == 0 ? 0
			: System.currentTimeMillis() - vacuum.getVacuumStarted();
}
```

now:

```
public long getTimeInVacuum() {
	long vacuumStarted = vacuum.getVacuumStarted();
	return vacuumStarted == 0 ? 0
			: System.currentTimeMillis() - vacuumStarted;
}
```

## mvcc - cache/volatile

Somehow I never believed in volatile - but see the changes to LiSet in 03/2025 - this _may_ mean that all entity fields
should be volatile (at the outside), or a rethink (sync?) on how version data is exposed.

Note that this issue makes a strong argument for moving commit for a local tx from the main commit queue thread to the
db commit thread - countervailing to that is 'what about coherency in the queue thread?'

### vscode

turn off lombok

## gwt

delete the gwt-unitCache every day, it (heuristically) incrementally slows page load
