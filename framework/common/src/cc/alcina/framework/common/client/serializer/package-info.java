/**
 * <p>
 * Notes re the package
 *
 * <p>
 * {@link ReflectiveSerializer} is intended for large graphs,
 * {@link FlatTreeSerializer} for small, tree-shaped graphs. So some of the
 * optimizations (particularly reflective/type lookups and population of
 * TypeNode/PropertyNode assist intsnaces in ReflectiveSerializer) wouldn't help
 * FlatTreeSerializer performance since there's not enough repetition to make a
 * difference.
 * 
 * <p>
 * Debugging notes
 * </p>
 * <p>
 * Most issues occur due to different type knowledge or type serialization
 * options/transience contexts between client + server - to debug, log the
 * serialized graph to file and then check deserialization with exactly the same
 * serialization opts and transience contexts.
 * <p>
 * Some can be caused by inconsistencies between the jvm and gwt type erasure
 * (and computation of Property.typeBounds)
 * <p>
 * Those issues can possibly be patched by specifying the element type in an
 * annotation structure - e.g.:
 * 
 * <pre>
 * <code>
&#64;TypeSerialization(
properties = { @PropertySerialization(
	name = "value",
	defaultProperty = true,
	types = View.Status.class) },
value = "status")
 * </code>
 * </pre>
 */
package cc.alcina.framework.common.client.serializer;
