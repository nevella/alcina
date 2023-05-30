/**
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
 *
 *
 *
 */
package com.google.gwt.dom.client;
