/**
 * Remote Dom allows the dom backing state (typically dirndl + environment) to
 * be maintained server-side, with just a thin shim supporting client
 * interaction
 * 
 * The goal is: to support a dynamic UI in the browser with no browser/js
 * recompilation necessary for UI model changes
 * 
 * <h3>The dom handshake</h3>
 * <ol>
 * <li>All packets contain environment uid, environment auth, client uid
 * <li>The client sends a 'register' packet with browser dom outer html and
 * localdom props
 * <li>The environment populates the server document with the browser dom
 * <li>The environment calls remoteui.init() which causes dommutations +
 * registrations (DomEvent)
 * <li>The server treats the client as synchronous (by blocking until ACK)
 * </ol>
 */
package cc.alcina.framework.servlet.dom;
