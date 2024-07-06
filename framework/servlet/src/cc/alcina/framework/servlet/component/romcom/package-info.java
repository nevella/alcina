/**
 * <p>
 * This remote object model component (romcom) module allows almost all dirndl
 * code to run on the server, with just a thin client to render dom + receive
 * events
 *
 * <p>
 * Much of the interaction is supported by the NodeRefid subtypes in the
 * {@link com.google.gwt.dom.client} package; the 'server hosting client' code
 * is mostly in {@link VmEnvironment}
 *
 * <p>
 * The main rpc protocol comms between server + client:
 *
 * <p>
 * Server sends mutation or invoke events to client [dom-mutation,
 * listener-mutation, urlhash-mutation, jsinvoke
 *
 * <p>
 * Client sends event-listener invokes to the server (receiver + event), as well
 * as urlhash-mutation
 *
 * <h4>Server environments
 * <p>
 * Normally, a new {@link VmEnvironment} is generated for each init() from the
 * client - the exception is a 'sole environment' endpoint, such as an
 * android-app-hosted web ui, where there is only one DOM layout hosted per
 * client (but see package {@link cc.alcina.framework.servlet.dom} - romcom
 * handshake - this is probably changing)
 *
 * <h4>Future
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
 * <h4>The event loop + synchronisation</h4>
 * <ul>
 * <li>Client sends a POST containing a json-serialized RemoteComponentRequest
 * </ul>
 * <h4>Limitations
 * <ul>
 * <li>Drag/capture is not supported (this is one place where you really want
 * client-side handling)
 * </ul>
 */
package cc.alcina.framework.servlet.component.romcom;
