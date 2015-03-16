### Introduction ###

Some of the decisions outlined below are questionable - if anyone has better ideas, please contribute!


### Questions ###

#### Why must the id field be a `long`, and not `java.io.Serializable` as per EJB3? ####
  * `long` rather than `int` because db tables do get large.
  * `long` rather than `Serializable` for performance and code simplicity - particularly wrt lookups on the client.
DomainTransforms use `UnsafeNativeLong` for serialization, so they're reasonably fast (except on IE :) )

#### Why mandate `MutablePropertyChangeSupport`? ####
  * Because the standard JVM `PropertyChangeSupport` class regards `firePropertyChange(String, null, null)` as a valid change event, bi-directional binding with that support gives an infinite loop.
  * The `fireNullPropertyChangeEvent` and static `muteAll()` methods can be extremely useful, in the right circumstances.
  * That said, it's definitely a hack.

#### Why the horrible locator pattern below, rather than say, Guice? ####
  * Didn't know about Guice until recently
  * I am thinking about, say, `PermissionsManger`.get() returning `PermissionsManagerSPI` (an interface) - thereby abstracting that totally - it's just that this way makes the code shortest and most readable IMO.

#### What gave you the idea for this? ####
  * Believe it or not, Microsoft Exchange. Only in a negative sense, in a sense, in the sense that I was dealing with some horrible Exchange backup problems  - and Exchange, like almost any other continuous db replication/backup system, uses ordered transform logs as its data exchange mechanism - at the same time as wandering around thinking "I hate persistence on all webapps i've ever worked on ..." - "bing!!" - serialized (in the sense of "order") client graph transforms.

#### And the name? ####
Pronounced al-Cheen-ah - I had no idea re the libretto, but seems appropriate:


> _The beautiful Alcina seduces every knight that lands on her isle, but soon tires of her lovers and changes them into stones, animals, plants, or anything that strikes her fancy._


G.F. Handel, cond: William Christie, orch: Les Arts Florissants, Morgana: Natalie Dessay -  _Tornami A Vagheggiar_
(p.s. don't bother listening on Amazon, they've got the track order wrong - but it is _to die for_)


### Patterns ###
#### Service location/lookup ####
  * When a class has two completely different implementations (JVM or GWT), it's accessed via `CommonLocator.get()`, e.g.
```
   CommonLocator.get().classLookup()
```
  * When a class has significant common code, (and is generally threadlocal on the server), it's accessed via a static `get()` call on the base class - essentially the base class is its own factory/locator, e.g.
```
    PermissionsManager.get()
    TextProvider.get()

    etc.

```
#### Wrapped runtime exceptions ####

In locations where application logic means that a checked exception should never occur in a method, _except_ due to programmer error, exceptions are wrapped in runtime exceptions to make upper-layer code cleaner.

This may be a little (or a lot) unJava, but it does make for simpler code - and possibly provides _better_ handling (in terms of "what the exception means to the application" - because you can centralise serious exception handling in  onuncaughtexception handler code, with a little help from `WrappedRuntimeException.SuggestedAction`

Excuses excuses. All lazy programmers, raise your hands...