/**
 * <p>
 * This remote object model component (romcom) module allows almost all dirndl
 * code to run on the server, with just a thin client to render dom + receive
 * events
 * 
 * <p>
 * Much of the interaction is supported by the NodePathref subtypes in the
 * {@link com.google.gwt.dom.client} package; the 'server hosting client' code
 * is mostly in {@link Environment}
 * 
 * <p>
 * The main rpc protocol comms between server + client:
 * 
 * <p>
 * Server sends mutation or invoke events to client [dom-mutation,
 * listener-mutation, urlhash-mutation, jsinvoke??(last not implemented yet)]
 * 
 * <p>
 * Client sends event-listener invokes to the server (receiver + event), as well
 * as urlhash-mutation
 * 
 * <p>
 * FIXME - remcom - check handling of dropped packets (client), multiple
 * tab/same url (environment)
 * <p>
 * FIXME - remcom - Tests Essentially, 'an app works' - but:
 * <ul>
 * <li>history mutation works (both sides)
 * <li>event emission works (client)
 * <li>event handling/dom mutation works (server)
 * </ul>
 */
package cc.alcina.framework.servlet.component.romcom;
