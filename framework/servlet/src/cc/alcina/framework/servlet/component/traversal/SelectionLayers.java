package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView.Ui;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.dom.Environment;

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
		Ax.out("History delta - id %s - %s", Environment.get().uid,
				Ax.ntrim(Ui.place(), 30));
		this.page = page;
		this.place = page.place;
		render();
	}

	void render() {
		if (page.history == null) {
			return;
		}
		traversal = Ui.traversal();
		List<LayerSelections> layers = traversal.getVisitedLayers().stream()
				.map(layer -> new LayerSelections(this, layer))
				.collect(Collectors.toList());
		layers.removeIf(layer -> !TraversalSettings.get().showContainerLayers
				&& layer.unfiliteredSelectionCount() == 0
				&& !layer.nameArea.hasFilter);
		setLayers(layers);
	}

	public void setLayers(List<LayerSelections> layers) {
		set("layers", this.layers, layers, () -> this.layers = layers);
	}

	boolean placeChangeCausesChange(TraversalPlace newPlace) {
		return !place.equivalentFilterTo(newPlace)
				|| !Objects.equals(place.viewPath().segmentPath,
						newPlace.viewPath().segmentPath);
	}
}
