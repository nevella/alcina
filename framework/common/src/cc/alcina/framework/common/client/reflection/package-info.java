/**
 * <h2>Alcina reflection</h2>
 * 
 * <h3>Goals</h3>
 * <ul>
 * <li>Equivalent reflection functionality and api for jvm and gwt code
 * <li>Reflection-based serializers
 * <li>Maximally readable code
 * <li>Remove redundancy where possible
 * </ul>
 * 
 * <h3>Implementation</h3>
 * <ul>
 * <li>Reflections api
 * <li>Single internals api (GWT typemodel for both GWT compilation and JVM
 * runtime)
 * 
 * </ul>
 * <h3>'Beans 1x5 manifesto'</h3>
 * <p>
 * Make beans pretty again!
 * </p>
 * <ul>
 * <li>No-args constructors not required
 * <li>Field-defined properties
 * <li>Package by default
 * <ul>
 * <li>The Java protection level mechanism default is the level at which -
 * presumably - the designers of Java originally envisioned most code would
 * exist.
 * </ul>
 * <li>Permit nested classes
 * </ul>
 * <p>
 * Level 2:
 * <ul>
 * <li>No-args _serializable_ bean constructors not required
 * </ul>
 */
package cc.alcina.framework.common.client.reflection;
