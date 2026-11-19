# LocalDom events - flows - worked example (DecoratorNode)

## >Mutation events

A worked example
**LocalMutation** - WIP, these are currently transactional, flushed by
`LocalDom#flushLocalMutations` or during flush-to-remote.
**TODO**
never flush to remote until the 'localmutation flush queue' is empty - this
allows cascading localmutations to cause only 1 remote mutation flush.

**TODO.2**

## FN

EditArea forwards dom mutations (fired by
LocalMutation) to the FragmentModel, which enqueues the transformed
FragmentModel.Mutation back on the area

## Note re handling of localmutations emitted during localmutation event dispatch

Any local mutations published during handling of local mutation events
will be called immediately after the current event(s) are published. So scheduleFinally() should
not be used to interleave local mutation dispatch runs (since it can't be guaranteed that it'll
be called between two dispatch runs)

## Note re immediate vs batched (transactional) localmutation listeners

- In general, err on the side of transactional since it gives the listener greater scope (the listener
  can view events in the context of other events)
- Invalidation listeners (e.g. DomNode.children, Location recomputes) should not be batched, since
  the computed value is invalid as soon as the relevant change occurs
