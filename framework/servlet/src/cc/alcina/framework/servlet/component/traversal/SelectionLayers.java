package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.environment.RemoteUi;

@Directed(tag = "layers")
class SelectionLayers extends Model.Fields {
	@Directed
	Heading header = new Heading("Selection layers");

	@Directed.Wrap("layers")
	List<LayerSelections> layers;

	Page page;

	SelectionTraversal traversal;

	TraversalPlace renderedPlace;

	SelectionLayers(Page page) {
		Ax.out("History delta - id %s - %s", RemoteUi.get().getUid(),
				Ax.ntrim(Ui.place(), 30));
		this.page = page;
		this.renderedPlace = page.ui.place;
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
		return !renderedPlace.equivalentFilterTo(newPlace)
				|| !Objects.equals(renderedPlace.viewPath().segmentPath,
						newPlace.viewPath().segmentPath);
	}
}
