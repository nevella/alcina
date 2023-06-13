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
 * Make beans pretty again! Beans of the world unite, you have nothing to lose
 * except your crusty boilerplate! Hello Portland!
 * </p>
 * <p>
 * TL;DR
 * 
 * <pre>
 * <code>

// A dirndl UI example
&#64;Bean
&#64;Directed
public static class HelloBeans1x0 {
	private String world = "World!";

	&#64;Directed
	public String getWorld() {
		return this.world;
	}

	public void setWorld(String world) {
		this.world = world;
	}
}

// A dirndl UI example
&#64;Bean(PropertySource.FIELDS)
&#64;Directed
static class HelloBeans1x5 {
	&#64;Directed
	String world = "World!";
}
<code>
 * </pre>
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
