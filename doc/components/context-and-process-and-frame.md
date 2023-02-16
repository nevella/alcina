# alcina > components > context-and-process-and-frame

note re context and observation:
* pipeline observation - modify tasks between pipeline stages
* context should be immutable where possible
* Explicit calls to central dispatch (ProcessObserver.publish) rather than annotation-based for observer dispatch
  is preferred because the kit overhead is much lower


notes re frame:

### ContextFrame
* The stack frame associated with a context (can be multiple if a context is replicated to multiple threads).
* Will be the top-level Thread.run() frame if the context is tied to a ThreadLocal

e.g. Transaction