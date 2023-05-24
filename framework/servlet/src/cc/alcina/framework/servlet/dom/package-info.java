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
 * <li>The client sends a 'register' packet with uid, auth and the browser dom
 * outer html
 * <li>The environment populates the server document with the browser dom
 * <li>The environment calls remoteui.init() which ...
 * </ol>
 */
package cc.alcina.framework.servlet.dom;
