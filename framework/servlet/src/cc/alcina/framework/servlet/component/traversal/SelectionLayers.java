package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;

@Directed(tag = "layers")
class SelectionLayers extends Model.Fields {
	@Directed
	Heading header = new Heading("Selection layers");

	static PackageProperties._SelectionLayers_LayersContainer layersContainer_properties = PackageProperties.selectionLayers_layersContainer;

	/*
	 * @Directed.Wrap is not used (rather, a container class) because it's a
	 * scroll container, so code wants access to the rendered element
	 */
	@Directed(tag = "layers")
	@TypedProperties
	class LayersContainer extends Model.Fields implements TransmitState {
		List<LayerSelections> layers;

		@Directed
		CollectionDeltaModel collectionRepresentation = new CollectionDeltaModel();

		LayersContainer() {
			bindings().from(this).on(layersContainer_properties.layers)
					.to(collectionRepresentation)
					.on(CollectionDeltaModel.properties.collection).oneWay();
		}
	}

	@Directed
	LayersContainer layersContainer = new LayersContainer();

	Page page;

	SelectionTraversal traversal() {
		return Ui.traversal();
	}

	SelectionLayers(Page page) {
		TraversalBrowser.Ui.logConstructor(this);
		this.page = page;
		bindings().from(page.ui).on(Ui.properties.traversal)
				.map(this::toLayerSelections).to(layersContainer)
				.on(layersContainer_properties.layers).oneWay();
	}

	List<LayerSelections> toLayerSelections(SelectionTraversal traversal) {
		List<LayerSelections> layers = traversal.getVisitedLayers().stream()
				.map(layer -> new LayerSelections(this, layer))
				.collect(Collectors.toList());
		layers.removeIf(layer -> !TraversalSettings.get().showContainerLayers
				&& layer.unfilteredSelectionCount() == 0
				&& layer.getLayerFilterAttribute() == null);
		layersContainer_properties.layers.set(layersContainer, layers);
		return layers;
	}
}
