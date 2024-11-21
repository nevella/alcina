package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.IfNotExisting;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables.ObservableHistory;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.environment.RemoteUi;

@Directed(tag = "layers")
class SelectionLayers extends Model.Fields implements IfNotExisting<HasPage> {
	@Directed
	Heading header = new Heading("Selection layers");

	/*
	 * @Directed.Wrap is not used (rather, a container class) because it's a
	 * scroll container, so code wants access to the rendered element
	 */
	@Directed(tag = "layers")
	class LayersContainer extends Model.All implements TransmitState {
		List<LayerSelections> layers;

		public void setLayers(List<LayerSelections> layers) {
			set("layers", this.layers, layers, () -> this.layers = layers);
		}
	}

	@Directed
	LayersContainer layersContainer = new LayersContainer();

	Page page;

	SelectionTraversal traversal;

	TraversalPlace renderedPlace;

	ObservableHistory renderedHistory;

	SelectionLayers(Page page) {
		TraversalBrowser.Ui.logConstructor(this);
		Ax.out("History delta - id %s - %s", RemoteUi.get().getUid(),
				Ax.ntrim(Ui.place(), 30));
		this.page = page;
		this.renderedPlace = page.ui.place;
		this.renderedHistory = page.history;
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
		layersContainer.setLayers(layers);
	}

	boolean placeChangeCausesChange(TraversalPlace newPlace) {
		return !renderedPlace.equivalentFilterTo(newPlace)
				|| !Objects.equals(renderedPlace.viewPath().segmentPath,
						newPlace.viewPath().segmentPath);
	}

	@Override
	public boolean testExistingSatisfies(HasPage input) {
		if (input.providePage().history != renderedHistory) {
			return false;
		}
		return placeChangeCausesChange(input.providePage().place());
	}
}
