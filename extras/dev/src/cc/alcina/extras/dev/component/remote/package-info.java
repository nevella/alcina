/**
 * <p>
 * This component allows almost all dirndl code to run on the server, with just
 * a thin client to render dom + receive events
 * 
 * <p>
 * Much of the interaction is supported by the NodePathref subtypes in the
 * {@link com.google.gwt.dom.client} package
 * 
 * <p>
 * The main rpc protocol comms between server + client:
 * 
 * <p>
 * Server sends mutation or invoke events to client [dom-mutation,
 * listener-mutation, urlhash-mutation, jsinvoke??]
 * 
 * <p>
 * Client sends event-listener invokes to the server (receiver + event)
 */
package cc.alcina.extras.dev.component.remote;
