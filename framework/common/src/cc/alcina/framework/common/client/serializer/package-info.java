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
 */
package cc.alcina.framework.common.client.serializer;
