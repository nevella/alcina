/**
 * <p>
 * Generates java files providing reflective access to types (one per type), and
 * the 'index class' (the module source file) providing access <i>to</i> those
 * types (by class lookup, forName and Registry)
 * 
 * <p>
 * The implementation was originally for GWT, then generalised to support
 * Android (and the jdk for testing)
 * 
 * <pre>
 * TODO
 *
 * - Get symbol analysis to work in obf
 *
 * - Cleanup async reflection reachability - possibly add a second reachability
 * doc (or just a text reason/path)
 *
 * - (gwt/jdk work):
 *
 * -- check refactoring works
 *
 * -- seutils -- remove properties
 *
 * -- class reflector - gen via classreflection
 * </pre>
 * 
 * <p>
 * Configuration
 * <ul>
 * <li>See
 * <code>[git-root]/alcina/framework/servlet/src/cc/alcina/framework/servlet/component/romcom/RemoteObjectModelComponentClient.gwt.xml</code>
 * as an example
 * <li>Run a gwt compilation pass
 * </ul>
 * 
 * <p>
 * The story so far...
 * <ul>
 * <li>(Repeat until no new reachable reflective types are encountered)
 * <li>Run a gwt compilation pass
 * <li>Generate the set of reachable reflective types as per javadoc of
 * ReflectionReachabilityLinker
 * </ul>
 * 
 * *
 * <p>
 * Empty params...
 * <ul>
 * <li>
 * </ul>
 * 
 * <p>
 * Cleaning...
 * <ul>
 * <li>Delete the generated types and types reasons files - contents of
 * ClientReflectionGenerator.ReachabilityData.folder
 * </ul>
 */
package cc.alcina.framework.entity.gwt.reflection;
