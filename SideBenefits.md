# Introduction #

The "hidden extras" (easter eggs?? ) of serializing domain transforms are what makes me compare Alcina to sliced bread...in my dreams


# Details #

### Undo support ###
_Not yet implemented, but really not hard_
  * On the client, it's trivialish, and there are already hooks - have a look at NullUndoManager
  * On the server, if you stick to altering class graphs in the (JPA) domain via domainTransforms, you can rewind them any point in time - simply replay from zero. Instant versioning...

### Provisional objects ###
In many client applications, the "save/discard" modality for user edits is fairly common. Alcina supports this with the concept of provisional objects - which have their transforms recorded (`RecordTransformListener`) but not committed to the client domain/model. Which is particularly important in terms of not committing changes to associated objects which are already part of the "committed" local domain.

Once the provisional object is "promoted", its transforms are replayed and committed to the graph via  `CommitToLocalDomainTransformListener` - and thence to local/remote storage.

This behaviour is supported throughout the project - both for very large provisional graphs (currently max is about 10,000 objects), and for annotation-ui-bound bean editing, where child (provisional) beans can be created on the fly - `ChildBeanCustomiser`

### Servlet layer ###
There are advantages to having a small JPA entity layer - one clear plus is the less code, the smaller the attack surface. So, performing large backend tasks, the following process:
  * Get needed objects from db via JPA
  * Work (potentially long-running) without a transaction lock in the servlet layer
  * Commit changes via `CommonRemoteServiceServlet.transformFromServletLayer()`
...well, I do it a lot. Another plus is the rewindable delta history.