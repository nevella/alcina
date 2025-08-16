# various dirndl notes, uncategorised

### intermediate listener aggregation

- table x - a model reachable by a each row property listens on event y (potentially 1000s of rows - so 1000s of listeners)
- rather than attaching listener to distant ancestor SOURCE(Y), attach to the parent, which passes it on as some wrapped event.
  When the parent detaches, there's no thundering herd of detachments (since its listeners are cleaned first)
- Note though that DirectedLayout.Node.children is still an arraylist-type structure. A real fix for O(n^2) removals would
  involve switching this to a linked structure
