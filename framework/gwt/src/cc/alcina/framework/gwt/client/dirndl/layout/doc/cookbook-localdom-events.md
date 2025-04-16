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
