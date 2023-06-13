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
 * representations - <i>local</i> (local to the .js or hostedmode java vvm) and
 * <i>remote</i> - which varies depending on the context but, in the case of a
 * GWT client is the browser DOM.
 * 
 * <p>
 * When the remote DOM representation is the browser DOM, the remote fields are
 * subtypes of {@link NodeJso}, which is where the code of the pre-localDom Node
 * classes was refactored to.
 * <h2>Invariants</h2>
 * <p>
 * LocalDom uses two parallel trees - 'local' (jvm/js) and 'remote' (browser
 * dom, or remote computer local/remote pair).
 * 
 * <p>
 * "Node" (unqualified) refers to the logical objects used (Element, Node, Text,
 * Document) etc which have references to both the local and remote trees, ditto
 * "tree" (refers to the logical tree reffing the local and remote). When nodes
 * are initially built, the remote node is set as a dummy (ElementNull) etc so
 * that writes have no effect on browser dom (and are thus fast). Traversal and
 * attribute checking are performed on the local node.
 * </p>
 *
 * <p>
 * For most logical purposes, the system requires that the trees be in sync. The
 * mechanisms used are:
 * <ul>
 * <li>The initial local tree is built from the html of the loading page (note -
 * traversing browser dom once and serializing as json is fast - even for large
 * trees. FIXME - localdom - switch to traversal-only, since we sometimes have
 * to fall back on it anyway)
 * <li>If a new local element is attached to an existing remote element, a
 * corresponding 'pending resolution' element is created and the local html tree
 * flushed at the end of the Scheduler event loop
 * <li>If a node with a real remote node is attached to a local-only node, that
 * local-only node (and its local-only parents) must be immediately flushed.
 * This is checked for prior to all writes on main-tree nodes
 * </ul>
 * </p>
 * <h2>Dealing with invalid incoming HTML</h2>
 * <p>
 * If there's a difference in node childcount/type between local and remote
 * correspondents, assume that the we're in html supplied via setHTML that was
 * invalid, and reparse. Use
 * com.google.gwt.dom.client.ElementJso.buildOuterHtml() if in IE (see its 'text
 * document'-centric html approach documented elsewhere). Can also be
 * preemptively validated via Element.setInnerHTMLWithValidation
 * </p>
 * <p>
 * TODO - remote the DomXxStatic - they may be removable with changes to the
 * devmode jso/wrapper generator (using default interface methods rather than a
 * shared static class)
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
 *
 *
 *
 */
package com.google.gwt.dom.client;
