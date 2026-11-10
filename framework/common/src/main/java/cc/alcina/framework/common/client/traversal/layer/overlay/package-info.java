/**
 * <p>
 * Manage overlays of a document range (modelled by {@link Measure} instances)
 * using a {@link FragmentModel} mapping of the range
 * 
 * <p>
 * Rough sketch:
 * <ul>
 * <li>Create a fragment model corresponding to the range
 * <li>Create an overlay (specify the range)
 * <li>Possibly expand the overlay - this will use properties of the
 * fragmentmodel
 * <li>Apply the overlay
 * <li>Note re wrap/strip - lower (element) level - preserve attachid over
 * strip/wrap
 * </ul>
 */
package cc.alcina.framework.common.client.traversal.layer.overlay;
