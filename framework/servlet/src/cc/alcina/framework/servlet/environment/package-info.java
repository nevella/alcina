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
 * (implementation - see FIXME - romcom )
 * 
 * *
 * <h3>Eventing and eventing gotchas</h3>
 * 
 * <pre>
 
  onInput - 20240219 - does not work (but onKeyDown does)

  Event sink (browser) happens here:
 com.google.gwt.user.client.impl.DOMImplStandard.sinkEvents(Element elem, int bits)

 Basically, it wasn't hooked up in the same way at bitted events - added to :
 com.google.gwt.dom.client.Element.putRemote(ClientDomNode remote, boolean synced)

 Even so, event hookup is -still- unclear and multi-pathed (obviously, the
  double hookup of local/patheref/browserlocal/jso is complicated) - it definitely needs a 
  "one path for all". There - possibly a simplification of GWT DOM/DOMImpl would make sense 
  (since there's now basically just one DOM - webkit/blink/gecko - are all similar enough )

  Notes - Window events are handled differently again (with some wrinkles for IE6) - basically, for ROMCOM 
  just use the PageHideEvent which is handled separately
 * 
 * 
 * 
 * 
 * 
 * </pre>
 * 
 * <h3>Message transport observation</h3>
 * <p>
 * To observe rmcp message lifecycle - to, say, get metrics on when Mutation
 * messages are dispatched (server), received (client) and processed (client),
 * observe the
 */
/*
 * RefIdDom DOM(s) are a server-side linked dom structure (NodeLooal, NodeRefId)
 * coupled to an in-browser dom structure (NodeLooal, NodeJso) via rpc calls -
 * the relationship is:
 *
 * Server.NodeLocal <--> Server.NodeRefId <==> Client.NodeLooal <-->
 * Client.NodeJso (Client.NodeJso being the 'real' browser dom). Essentially all
 * the interest occurs in Server.NodeLocal and Client.NodeJso - the other two
 * structures are used purely for synchronisation.
 *
 * 'NodeRefId' is so-called because the server has no object refs to client
 * nodes, instead each created node is assigned a uniqueid - with disjoint id
 * sequences for [created-server-nodelocal] and [created-client-nodejso] -
 * except for the html documentElement (id === 1, both ends)
 * 
 * 
 */
/*
 * Session handling logic
 * 
 * There are basically three types of apps - single UI only (say an android app
 * with a webview UI), multiple UI (the sequence browser is one such - server
 * logic is confined to the UI thread itself), and replaceable single UI (where
 * only one UI instance can exist, but a browser refresh returns a new,
 * connected UI rather than failing).
 * 
 * TODO - how is this modelled, how is it handled and what are some examples?
 */
package cc.alcina.framework.servlet.environment;
