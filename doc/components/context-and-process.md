# alcina > components > context-and-process

note re context and observation:
* pipeline observation - modify tasks between pipeline stages
* context should be immutable where possible
* Explicit calls to central dispatch (ProcessObserver.publish) rather than annotation-based for observer dispatch
  is preferred because the kit overhead is much lower



