/**
 *
 * <h2>The great heist of the GWT dom</h2>
 * <p>
 * The GWT dom is completely rewritten in Alcina. Prior to the rewrite, the
 * Element, Node etc objects were javascript object wrappers, meaning every DOM
 * mutation resulted in a write to the browser DOM.
 * <p>
 * With the rewrite, a {@link com.google.gwt.dom.client.Node} (now also
 * implementing {@link org.w3c.dom.Node}) is essentially a router for two DOM
 * representations - <i>local</i> (local to the .js or hostedmode/server java
 * vvm) and <i>remote</i> - which varies depending on the context but, in the
 * case of a GWT client is the browser DOM.
 *
 * <p>
 * When the remote DOM representation is the browser DOM, the remote fields are
 * subtypes of {@link NodeJso}, which is where the code of the pre-localDom Node
 * classes was refactored to.
 * <h2>Invariants</h2>
 * <p>
 * LocalDom uses two parallel trees - 'local' (jvm/js) and 'remote' (browser
 * dom, or server-side local/remote.attachId(proxy) pair).
 *
 * <p>
 * "Node" (unqualified) refers to the logical objects used (Element, Node, Text,
 * Document) etc which have references to both the local and remote trees, ditto
 * "tree" (refers to the logical tree reffing the local and remote). When nodes
 * are initially built, the remote node is null so that writes have no effect on
 * browser dom (and are thus fast). Traversal and attribute checking are
 * performed on the local node.
 * </p>
 *
 * <p>
 * Before exiting a top-level call affecting the dom, and at times during
 * processing, the system requires that the trees be in sync. The mechanisms
 * used are:
 * <ul>
 * <li>The initial local tree is built from the actual html of the loading page
 * (note - this process requires a constant number of calls to the browser dom,
 * so is non-prohibitive in devmode. This is true of all interactions with the
 * browser dom, due to extensions of the GWT devmode protocol).
 * <li>If a new local element is attached to an existing remote element, a
 * corresponding 'pending resolution' element is created and the local html tree
 * flushed at the end of the Scheduler event loop
 * <ul>
 * <li>The implementation tracks the 'attachId' of each local node. This ID is
 * set recursively when a node is descent-reachable from the document element
 * ('attached') ('connected' is the browser DOM term) and removed when the
 * element is detached. The ID is also set/removed on NodeJso objects during
 * sync - a welcome side effect is that tracking the effect of mutations on the
 * whole DOM becomes easy, sidestepping the issue of "mutations while detached".
 * </ul>
 * </ul>
 * </p>
 * <h2>Linked DOM types</h2>
 * <ul>
 * <li><b>Local-only</b> ('one-way') For server-side html rendering
 * <li><b>Browser</b> ('two-way') The DocumentLocal tree is synced with a
 * DocumentJso tree
 * <li><b>Romcom</b> ('four-way') Two local/remote pairs - server (owned by the
 * app instance's Environment), with a DocumentLocal/DocumentAttachId pair;
 * synced to browser (DocumentLocal/DocumentJso)
 * </ul>
 * <h2>Dealing with invalid incoming HTML</h2>
 * <li>For the two-way linkage, it's easy - always prefix any setInnerHtml
 * (local) call via setInnerHtml (remote)/getInnerHtml (remote), which will
 * ensure the markup is valid (e.g. <code>&lt;table&gt;&lt;tr&gt;</code> markup
 * would be replaced by <code>&lt;table&gt;&lt;tbody&gt;&lt;tr&gt;</code>).
 * <li>For four-way - it's a TODO WIP - but basically the client-side mutation
 * should signal that the server-side structure is invalid, with the correct
 * markup - and the server-side DOM should emit some sort of synthetic NodeEvent
 * <p>
 * TODO - basically, if there's a difference in node childcount/type between
 * local and remote correspondents, assume that the we're in html supplied via
 * setHTML that was invalid, and reparse. Use
 * com.google.gwt.dom.client.ElementJso.buildOuterHtml() if in IE (see its 'text
 * document'-centric html approach documented elsewhere). Can also be
 * preemptively validated via Element.setInnerHTMLWithValidation
 * </p>
 * 
 * 
 * <h2>Implementation notes</h2>
 * <ul>
 * <li>Why DomXxStatic? Because the devmode jso generator doesn't allow
 * non-final methods on interfaces TODO - remote the DomXxStatic - they may be
 * removable with changes to the devmode jso/wrapper generator (using default
 * interface methods rather than a shared static class)
 * <li>TODO - check doc/frame constructor complexity
 * <li>TODO - document the 'why' of the 'validate' methods in Node.java
 * 
 * </ul>
 * <h2>The story of an event listener registration</h2>
 * <p>
 * Say there's a dirndl model with a
 * {@code @Directed...receives = DomEvents.Click.class} annotation - here's the
 * registration and event dispatch process [for local-only dom nodes] :
 * <ul>
 * <li>Directed layout:
 * <ul>
 * <li>{@code DirectedLayout.Node.bindBehaviours() -> create a NodeEventBinding, call bind()}
 * <li>{@code  NodeEventBinding.bind() -> DomBinding, call bind()}
 * <li>{@code  DomBinding.bind() -> DomEvents.Click.BindingImpl.bind1()}
 * <li>{@code  DomEvents.Click.BindingImpl.bind1() -> Widget.addDomHandler(x,ClickEvent.getType())}
 * <li>{@code   Widget.addDomHandler(x,ClickEvent.getType()) -> Widget.sinkEvents}
 * <li>...
 * <li>{@code   Widget.onAttach() -> elem.uiObjectListener = [widget]listener;}
 * <li>{@code   Widget.onAttach() -> Widget.sinkEvents;}
 * <li>{@code   Widget.sinkEvents() -> (nearest jso ancestor) add js event handler;}
 * </ul>
 * <li>LocalDom.flush():
 * <ul>
 * <li>FIXME - the sinkEvents calls could be collated and only applied on
 * flush() - (nearest jso ancestor to node has jso event listener, since
 * registration target had no remote - near)
 * </ul>
 * <li>Click:
 * <ul>
 * <li>{@code DOM.dispatchEventImpl -> bubble up from target }
 * <li>if {@code childElement == original :: add to dispatch list}
 * </ul>
 * </ul>
 * <h4>DOM processes</h4>
 * <h5>Attach</h5>
 * <p>
 * Formerly GWT widget-only, this process is now mostly moved to Element
 * (responsible for bind/unbind dom event handlers). It's possible (TODO -
 * dirndl) that event handler binding could be further simplified in the future,
 * by merging 'bitless' and 'bitted' - but not yet.
 * <p>
 * On bootstrap, the &lt;html&gt; element is marked as attached, and its
 * descendants at the time are attached by recursive calls. Subsequently, any
 * call to setParent will check the attached state of the parent and propagate
 * if there's a delta
 * <h5>local --&gt; remote flush</h5>
 * <ul>
 * <li>When an element is attached to DOM, if the parent has an existing remote
 * mapping, the child is marked as 'requiring flush' and a finally task is
 * scheduled to 'flush' the element.
 * <li>This means that (in general) only one element requires remote writes -
 * the initial attach and the setInnerHtml performed by the flush task, which
 * writes the subtree html to the remote node
 * <li>(TODO) - attachId vs jso
 * <li>(TODO) - model more formally (as an algorithm sketch)
 * </ul>
 * <h4>Widget -&gt; Element migration</h4>
 * <p>
 * In GWT, the Widget system is the point at which client code registers DOM
 * event listeners. Because Dirndl eschews widgets (as unnecessary),
 * {@link Element} now also contains a listener registration point
 * ({@link HandlerManager} and implements the same interfaces
 * ({@link HasHandlers}, {@link EventListener}) as Widget to allow _either_
 * system to be used. Note that for a given element, only one registration
 * system (Widget or Elemnent) can be used. It ain't pretty, but it's required
 * until Widget is removed from the codebase
 *
 * 
 * *
 * <h2>Mutation events</h2>
 * <p>
 * A worked example
 * <ul>
 * <li><b>LocalMutation</b> - WIP, these are currently transactional, flushed by
 * {@link LocalDom#flushLocalMutations()} or during flush-to-remote. TODO -
 * never flush to remote until the 'localmutation flush queue' is empty - this
 * allows cascading localmutations to cause only 1 remote mutation flush. TODO.2
 * - give examples (DecoratorNode) TODO - split out into cookbook + dom
 * mutations
 * <li><b>FragmentModel</b> EditArea forwards dom mutations (fired by
 * LocalMutation) to the FragmentModel, which enqueues the transformed
 * FragmentModel.Mutation
 * </ul>
 * 
 * 
 * <p>
 * See also {@link AttachId}
 * 
 * <h2>Topics - TODO</h2>
 * <ul>
 * <li>Event listener/registration/firing (possibly covered above)
 * <li>Debug/tracing tools
 * <li>Local mutations
 * <li>Attached + connected
 * <li>Document context + frame
 * </ul>
 *
 */
package com.google.gwt.dom.client;
