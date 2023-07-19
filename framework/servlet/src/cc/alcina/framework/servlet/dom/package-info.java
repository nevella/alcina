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
 * <h3>Romcom handshake v2 (WIP)</h3>
 *
 * <pre>
 * ### romcom handshake

  - client: /feature-tree
  - server: authenticate (optional) (alcina-servlet or querystring auth)
  - server: serve bootstrap html
  	- create environment
  	  - if ui is 'single instance only', invalidate other environments with same component class
  	- inject initial rpc connection parameters [session]
  	  - component (remoteui classname)
  	  - initial url
  	  - environment id (rename any use of 'session' to 'environment')
  	  - environment auth
  - client:
  	- post bootstrap packet to server
  	[yep - now fix Element/EventListener]
  - server:
  	- environment enters state 'connected' (but this may already be handled)(throw if already bootstrapped)
  - client:
  	- send heartbeat/observe mutation packet (with backoff on unreachable/404)
  	  - component
  	  - env id
  - server: (backoff)
  	- if env does not exist or is invalidated:
  	  - if single instance and another instance of the same type exists:
      	- reply with 'expired' (message includes 'will invalidate any other tabs viewing this component')
  	  - else:
  	    - reply with 'refresh'
  	- else:
  	  - await, reply with server event
 * </pre>
 *
 * (implementation - see FIXME romcom)
 */
package cc.alcina.framework.servlet.dom;
