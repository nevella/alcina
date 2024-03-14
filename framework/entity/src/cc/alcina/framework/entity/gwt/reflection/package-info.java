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
 */
package cc.alcina.framework.entity.gwt.reflection;
