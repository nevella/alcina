### Terminology ###
  * **Dataobject** - An object encapsulating a row in a database table - e.g. a JPA entity bean
  * **Client app** - in this case, a GWT javascript app running in a browser
  * **DAO**, **DTO** - Data access and data transfer object - used in many application patterns (particularly J2EE), according to some schools of thought these are what the client app should see/manipulate instead of the dataobjects themselves. Decent definitions in [Wikipedia](http://en.wikipedia.org/wiki/Data_Access_Object)
  * **Object graph** -  Interconnected objects, connected via object reference
  * **Persistent model** - an object graph composed of dataobjects
  * **Detached object** - an object which does not have any reference to a database/JPA entitymanager
  * **Gruff** - Gwt 2.1 ReqUestFactory/record/sync Framework


# Introduction #

The short form - similar results, different routes, but...sorta different fundamental goals - so ...sorta different. Alcina is designed for African internet connections as the worst case. Gruff is a little less tolerant of noise on the line. Gruff supports a more general server persistence model, Alcina starts with the assumption that most interesting modifications of the persistence model will take place on the client, so limits the persistence model but allows for more natural coding of model changes. And Alcina tries to observe the premises of the [Manifesto](Manifesto.md)


# Feature comparisons #

| **Item** | **Alcina** | **GWT propogation framework** |
|:---------|:-----------|:------------------------------|
| Source code complexity | Gnarly, large implementation - but no boilerplate.  | Really nice and clear - but lots of boilerplate (SpringRoo should hide/facilitate much of this)|
| Reflection | Java-style, but via a cross-platform locator lookup pattern rather than `Class.getAnnotations()`  etc| GWT (SpringRoo auto-generated/rebound class)-style |
| Change observation | Y'olde `PropertyChangeListener` | Via DTO |
| Client <> Server object association | Direct (deals directly with dataobjects) | Indirect (2-layer with DTO - e.g. `EmployeeRecord, Employee`) |
| Speed (client rendering) | Currently slow (table renderers use widgets) - could be optimised | About as fast as possible in HTML/JS |
| Speed (delta propogation, client sync) | Slower - but not that important, relatively speaking. Also easily optimised HTTP  (see some of the wire formats in the Alcina `protocolhandlers` package) | Optimalish |
| Permissions | Annotation-style, object and property level | Not addressed |
| JPA clipping | Addressed | Addressed - sort of. The Record objects aren't part of an object graph, just analogues to db records, really - i.e. they have id field references to other objects, rather than references to other Records (e.g.  `ExpenseRecord.getReportId()` ) - which is sort of anti-OO.|
| Offline support | Yes | Not currently supported, could be with a master sync manager and server persistence of sync requests...and an analogue of Class.newInstance() ... and a few more bits'n'bobs|
| Default client change sync type | Non-blocking | Blocking - would need some refits to not support this, see below (although I'd imagine that's totally planned) |
| Editable persistent object requirements | `HasIdAndLocalId, SourcesPropertyChangeEvents` | An id field |
| Multiple-object provisional edits | Supported | Not really supported, but probably could be  |

# Conclusion #

Honestly, lots of reasons to go with GWT2.1 -  but ... but ... I think it doesn't go far enough, and misses some of the real potentiality of a client/server app, which is that the client and the server be, as far as possible, equals.

For many modern client apps, the interesting  "business logic" (I _don't_ like the term) actually takes place on the client, not the server, so having the client's persistent object model (which I find myself manipulating/accessing all the time on the client) be a second-class citizen seems the wrong direction to take, to me.  _This may well be a misunderstanding of the long-term intent of  Gruff - after all, I'm only working off some early-committed code - if so, very sorry and I take it all back._

Before getting into that, as an aside, have a look at the first conversation in the [Manifesto](Manifesto.md), "out of order requests". Gruff seems to sidestep this only by blocking on model (Record) sync submission - of course, it could be extended the same way as Alcina to work asynchronously, note that that would require all sync (client > server) requests to have an incremental id and include prior unacknowledged requests.

Coming back to the persistence model, the use of in Gruff of DTOs and what's essentially two parallel models (in the expenses example, `EmployeeRecord/Employee, ReportRecord/Report etc`) is a J2EE pattern that's, I think, outdated, for reasons detailed in the [Manifesto](Manifesto.md), and has not been copied in many other client/server systems - OK, MSDN recommends it, but your various rapid development tools/languages, say Ruby/Rails or Python/Django, definitely don't go that way.

> Note: the classic argument "a DAO _(note, not a DTO)_ abstracts the view from the underlying data model" may be useful for very large systems - but if so, with a decent IDE there's nothing to prevent you applying a facade to the underlying dataobject in 5 minutes, _when it becomes necessary_. Until that point, the DAO/DTO will have an identical property (not necessarily method) signature to the DO - and note yet another beauty of GWT - your "business" logic will be compiled out if it's not used on the client, thereby getting rid of associated security concerns.

Also, given that Gruff is _almost_ a reflection framework (it has a Property object!!), there's less and less reason not to go all out and say, "OK, reflection is evil...we aren't evil...much...except when...ooh, it's just too much fun not to be" and just provide a curtailed form of reflection, as Alcina/Gwittir do - there's a lot of meta-information that the client needs to know that is (much) more elegantly conveyed via annotations and reflection, rather than via the use of two parallel (XML or Java, doesn't fundamentally matter) structures - as people discovered moving from EJB2 to EJB3. Introspection is the most natural (in the Java language) way to work with objects at the property level - and that's what property validation, sync and rendering involve.

Note that these examples of "introspection is the natural modality" aren't in conflict with the fact that the object graph serialization/deserialization classes generated by the  GWT-RPC rebinder are definitely a better implementation than a similar client-side reflective solution would be (speeeed!!! ). Serialization deals with whole objects (always) - always accessing _all_ the non-transient fields, wheras sync, property validation and property rendering operate at the property level, where the properties of a given object are independent of each other.

But...having thought about it overnight - there are two advantages to Gruff that may or may not be killer - depends on the app, really - they refer to object loading:

  1. Record <> Record references are "lazy-loading" - they're id refs and presumably will be evaluated by a lookup call to a ValueStore somewhere. So data is loaded on-demand, and the client will never run into errors because of detached object graph clipping. The con of that though, is the same as JPA lazy initializers ... you may end up with an exponential number of HTTP calls to populate objects. And given you should try and avoid JPA lazy initializers with great vim in a production app, that should apply, only more so, in the case of lazy-loading client objects.
  1. the record/DTO paradigm will use fewer bytes.

> Well - object loading is a whole other topic, sort of covered in the Alcina `ClientHandshakeHelper` class - currently, Alcina assumes apps follow the "load a big chunk of objects on init/login, then you can work offline/disconnected fairly successfully" paradigm - this currently causes slower initial load times for clients without fast connections, but since Alcina apps are build around the possibility of domain synchronisation, future app loads from an HTML5/Gears  client will only require loading the observed model delta (note this isn't implemented yet) (as of September 2010 - implemented - see `MixedGwtTransformHelper` et al).

### Really opinionated and prejudiced Gruff wishlist ###
  * Gruff should use real introspection (i.e. `java.lang.Annotation, Method, java.beans.PropertyDescriptor`)  _on the client._
  * Gruff, if non-blocking model modifications are intended, must take into account the possible sync failure cases, particularly where extra unwanted objects could be created on the db.
  * Lose the `SpringRoo`/IDE support "optional" requirement - which would make the Emacs crew real happy. That probably requires introspection and direct model maniuplation (as opposed to via DTO).
  * Alcina should look at losing the long id field requirement - possibly the Gruff string id (which can encapsulate both db id and local/future id) is the way to go - client performance for big object graphs is a question here though.
  * Alcina has a more general search/object spec system (see `SearchDefinition`, compared to Gruff's `EmployeeRequest` etc), the more general/semi-declarative form is probably a better basis for a polling/websockets continually running server > client model change observation and sync.

Yeah well - given the quality of GWT and its programmers , I'm sure Gruff will end up addressing all the above issues and be a better solution than Alcina (except maybe in some specialised cases), so ... 1-2-3 ... **_Viva Gruff!_**

p.s. JSON as the wire format is just temporary, right?