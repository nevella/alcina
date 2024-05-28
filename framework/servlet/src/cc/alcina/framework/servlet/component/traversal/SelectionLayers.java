package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView.Ui;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;

@Directed(tag = "layers")
class SelectionLayers extends Model.Fields {
	@Directed
	Heading header = new Heading("Selection layers");

	@Directed.Wrap("layers")
	List<LayerSelections> layers;

	Page page;

	SelectionTraversal traversal;

	TraversalPlace place;

	SelectionLayers(Page page) {
		Ax.out("History delta - id %s", Ui.get().getEnvironment().uid);
		this.page = page;
		this.place = page.place;
		render();
	}

	void render() {
		if (page.history == null) {
			return;
		}
		traversal = page.history.observable;
		DepthFirstTraversal<Layer> renderTraversal = new DepthFirstTraversal<Layer>(
				traversal.getRootLayer(), Layer::getChildren, false);
		List<LayerSelections> layers = renderTraversal.stream()
				.map(layer -> new LayerSelections(this, layer))
				.collect(Collectors.toList());
		layers.removeIf(layer -> !TraversalSettings.get().showContainerLayers
				&& layer.unfiliteredSelectionCount() == 0);
		setLayers(layers);
	}

	public void setLayers(List<LayerSelections> layers) {
		set("layers", this.layers, layers, () -> this.layers = layers);
	}

	boolean placeChangeCausesChange(TraversalPlace newPlace) {
		return !place.equivalentFilterTo(newPlace);
	}
}
