/**
 * <p>
 * A layout system that minimises repetition by using structural metadata
 * implicit in the DOM and class hierarchy trees.
 *
 * <h2>Debug notes</h2>
 * <p>
 * <i>(May be replaced by ProcessInspection)</i>
 * </p>
 * <ul>
 * <li>How do I debug the resolution of @Directed annotations at a point in the
 * rendered node?
 * <ul>
 * <li>Add a computed breakpoint in {@link DirectedMergeStrategy#atClass},
 * checking the reflected class of the ClassReflector
 * </ul>
 * </li>
 * </ul>
 * <h2>Speed notes</h2>
 * <ul>
 * <li>In say {@link ContextResolver#getRenderer}, does it make sense to cache
 * generated instances? Or are GC/instantiation basically as fast as caching
 * these immutables?</li>
 * </ul>
 *
 * @see doc/dirndl-cookbook.html
 */
package cc.alcina.framework.gwt.client.dirndl.layout;
