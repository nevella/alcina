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
 * </ul>
 */
package cc.alcina.framework.gwt.client.dirndl.model.fragment.overlay;
