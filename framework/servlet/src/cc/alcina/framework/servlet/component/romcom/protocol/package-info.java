/**
 * <h3>The client-server protocol for remotecomponents.</h3>
 * 
 * <p>
 * There aren't many messages - there are a few fixmes:
 * 
 * <ul>
 * <li>DOM mutation - element addressing needs a generation (mutation index) id
 * - since client events can occur on DOM the server has moved on from. Server
 * needs to be able to handle that (or initially ignore if the id doesn't match
 * current)
 * <li>There needs to be a generalised invokejs method (for iframe opts,
 * basically)...coming soon
 * </ul>
 */
package cc.alcina.framework.servlet.component.romcom.protocol;
