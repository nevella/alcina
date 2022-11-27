/**
 * <h2>Alcina execution environments</h2>
 * <p>
 * Alcina has essentially three execution environments: servlet (J2EE web app),
 * console (standalone jvm), client (gwt jvm or script). As far as is feasible,
 * the environments support the same set of services to minimise code
 * duplication. Each environment has different strengths and weaknesses for use
 * during development - the following is an approximate list:
 * <table>
 * <tr>
 * <th>Environment</th>
 * <th>Description</th>
 * <th>Advantages</th>
 * <th>Disadvantages</th>
 * </tr>
 * <tr>
 * <td>Webapp/servlet</td>
 * <td>Current supported implementation: Jboss Wildfly 24. Serves all server
 * code for the project</td>
 * <td>
 * <ul>
 * <li>All servlets are handled by this enviroment
 * <li>Restart updates db schema
 * <li>Provides access to the whole persistent domain
 * </ul>
 * </td>
 * <td>
 * <ul>
 * <li>Slow reload
 * <li>Very minimal code hot-swapping
 * </ul>
 * </td>
 * </tr>
 * <tr>
 * <td>Devconsole</td>
 * <td>A console app (with web gui) which allows execution of all server code
 * for the project, plus console-only (dev support) code</td>
 * <td>
 * <ul>
 * <li>Fast restart, better hot-swapping
 * <li>Supports rerun of last task (for incremental dev of a specific task)
 * (TODO: e.g.)
 * </ul>
 * </td>
 * <td>
 * <ul>
 * <li>Only RPC servlets are handled by this enviroment (see
 * {@link RemoteInvocationServlet}
 * </ul>
 * </td>
 * </tr>
 * <tr>
 * <td>Client</td>
 * <td>The application client</td>
 * <td>
 * <ul>
 * <li>Fast restart (page refresh essentially recompiles the app)
 * </ul>
 * </td>
 * <td>
 * <ul>
 * <li>Provides access to only a subset of the persistent domain
 * </ul>
 * </td>
 * </tr>
 * </table>
 * 
 * <p>
 * Generally, server development of server-only code (publications, jobs) should
 * be done within the devconsole. For server-side RPC code, it's slightly more
 * of a tossup, but if the EJB code is unchanged (i.e. code accessed via a
 * persistent bean), and the work is reasonably significant, devconsole will be
 * more productive for say more than 15 minutes work.
 * 
 * <p>
 * TODO: devconsole configuration and invocation
 * 
 */
package cc.alcina.framework.servlet;
