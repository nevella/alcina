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
 * Make beans pretty again. Beans of the world unite, you have nothing to lose
 * except your crusty boilerplate. And so on
 * 
 * <p>
 * TL;DR
 * 
 * <pre>
 * <code>

// A dirndl UI example (Beans 1x0 - java beans spec)
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

// A dirndl UI example Beans 1x5
&#64;Bean(PropertySource.FIELDS)
&#64;Directed
static class HelloBeans1x5 {
	&#64;Directed
	String world = "World!";
}
</code>
 * </pre>
 * </p>
 * <ul>
 * <li>No-args constructors not required
 * <li>Field-defined properties
 * <li>Package by default
 * <ul>
 * <li>
 * <p>
 * The Java protection level mechanism default is the level at which -
 * presumably - the designers of Java originally envisioned most code would
 * exist. Most java code uses either private or public (rather than default/no
 * access modifier) - essentially private by default (except for beans methods).
 *
 * <p>
 * Alcina is moving towards 'package by default' - as with private, the onus is
 * on the caller to verify that api usage is correct, if the method/field is not
 * documented. The motivation for this is to simplify the java code and provide
 * consistent protection semantics at the package level - see 'From private to
 * package' below
 * </ul>
 *
 * </ul>
 * <h4>Stage 2 (WIP):</h4>
 * <ul>
 * <li>No-args _serializable_ bean constructors not required
 * </ul>
 * <h4>From private to package</h4>
 * <h4>Notes</h4>
 * <ul>
 * <li>
 * <p>
 * Private signifies 'only classes in this nest can access the member', but
 * given top-level classes can contain a nested class structure of arbitrary
 * depth, the chance of incorrect member access of a private member (by say a
 * nested class) is still significant. For this reason, Alcina-style code style
 * is package-by-default, with the requirement that members with non-obvious
 * access semantics should be javadoc-documented if they're intended to support
 * access from outside the exact owning class (not the nest).
 * <p>
 * Exceptions to package-by-default are:
 * <ul>
 * <li>if the field has an ensure accessor
 * </ul>
 * <li>The other benefit of private is that static analysis tools (e.g. the mvcc
 * ClassTransformer) can make guarantees about access (since in java a class
 * cannot be extended). Package access allows no such guarantees. For this
 * reason, {@link Entity} subclasses retain the JavaBeans 1.0 property style.
 * </ul>
 * <h4>Property Fields</h4>
 *
 * Effectively, any non-private field not annotated with @Property.Not is an
 * Alcina Property - a superset of Java Beans properties
 */
package cc.alcina.framework.common.client.reflection;
