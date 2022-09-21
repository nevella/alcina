/**
 * <p>
 * A layout system that minimises repetition by using structural metadata
 * implicit in the DOM and class hierarchy trees.
 *
 * <h2>Debug notes</h2>
 * <ul>
 * <li>How do I debug the resolution of @Directed annotations at a point in the
 * rendered node?
 * <ul>
 * <li>Add a computed breakpoint in {@link DirectedMergeStrategy#atClass},
 * checking the reflected class of the ClassReflector
 * </ul>
 * </li>
 * </ul>
 */
package cc.alcina.framework.gwt.client.dirndl.layout;
