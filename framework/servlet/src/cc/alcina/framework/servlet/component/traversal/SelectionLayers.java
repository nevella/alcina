package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.CollectionUtil;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.IfNotEqual;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.LayerSelections.LayerEquivalence;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;

/**
 * <p>
 * Equivalence: two SelectionLayers areas are equivalent (so no property change)
 * if the defined filters in the place are the same.
 * 
 * <p>
 * This forces a redraw iff the place filters change
 */
@Directed(tag = "layers")
class SelectionLayers extends Model.Fields implements IfNotEqual {
	/*
	 * @Directed.Wrap is not used (rather, a container class) because it's a
	 * scroll container, so code wants access to the rendered element
	 */
	@Directed(tag = "layers")
	@TypedProperties
	class LayersContainer extends Model.Fields implements Model.TransmitState {
		List<LayerSelections> layers;

		/*
		 * This (rather than just the layers property/field) supports
		 * incremental rendering of the list
		 */
		@Directed
		CollectionDeltaModel collectionRepresentation = new CollectionDeltaModel();

		LayersContainer() {
			bindings().from(this).on(layersContainer_properties.layers)
					.to(collectionRepresentation)
					.on(CollectionDeltaModel.properties.collection).oneWay();
		}

		List<? extends Selection> getFilteredSelections(Layer layer) {
			Optional<LayerSelections> match = layers.stream()
					.filter(ls -> HasEquivalence.areEquivalent(
							LayerEquivalence.class, layer, ls.layer))
					.findFirst();
			return match
					.map(ls -> ls.selectionsArea == null ? null
							: ls.selectionsArea.tableSelections)
					.orElse(List.of());
		}
	}

	static PackageProperties._SelectionLayers_LayersContainer layersContainer_properties = PackageProperties.selectionLayers_layersContainer;

	@Directed
	Heading header = new Heading("Selection layers");

	@Directed
	LayersContainer layersContainer = new LayersContainer();

	@Directed
	Object spacer = new Object();

	@Override
	public int hashCode() {
		return 1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SelectionLayers) {
			SelectionLayers o = (SelectionLayers) obj;
			return place.equivalentFilterTo(o.place);
		} else {
			return false;
		}
	}

	Page page;

	TraversalPlace place;

	SelectionLayers(Page page) {
		this.page = page;
		this.place = page.place();
		bindings().from(page.ui).on(Ui.properties.traversal)
				.map(this::toLayerSelections).ifNotEqual().to(layersContainer)
				.on(layersContainer_properties.layers).oneWay();
	}

	SelectionTraversal traversal() {
		return Ui.traversal();
	}

	List<LayerSelections> toLayerSelections(SelectionTraversal traversal) {
		List<LayerSelections> layers = traversal.getVisitedLayers().stream()
				.map(layer -> new LayerSelections(this, layer))
				.collect(Collectors.toList());
		layers.removeIf(layer -> !TraversalSettings.get().showContainerLayers
				&& layer.unfilteredSelectionCount() == 0
				&& layer.getLayerFilterAttribute() == null);
		layers = CollectionUtil.preferExisting(layersContainer.layers, layers);
		return layers;
	}

	List<? extends Selection> getFilteredSelections(Layer layer) {
		return layersContainer.getFilteredSelections(layer);
	}
}
