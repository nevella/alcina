package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

class LayerSelections extends Model.All {
	@Directed(className = "bordered-area")
	class NameArea extends Model.All {
		String key;

		String outputs;

		NameArea() {
			FormatBuilder keyBuilder = new FormatBuilder();
			keyBuilder.indent(layer.depth());
			keyBuilder.append(layer.getName());
			key = keyBuilder.toString();
			outputs = computeOutputs();
		}
	}

	static class Spacer extends Model {
	}

	class SelectionsArea extends Model.All {
		@Directed(className = "bordered-area")
		class SelectionArea extends Model.All
				implements DomEvents.Click.Handler {
			String pathSegment;

			String text;

			private Selection selection;

			SelectionArea(Selection selection) {
				this.selection = selection;
				pathSegment = selection.getPathSegment();
				text = Ax.ntrim(Ax.trim(selection.get().toString(), 100));
			}

			@Override
			public void onClick(Click event) {
				event.reemitAs(this, TraversalEvents.SelectionSelected.class,
						selection);
			}
		}

		List<Object> selections;

		SelectionsArea() {
			selections = selectionLayers.traversal.getSelections(layer).stream()
					.limit(5).map(SelectionArea::new)
					.collect(Collectors.toList());
			for (int idx = selections.size(); idx < 5; idx++) {
				selections.add(new Spacer());
			}
		}
	}

	NameArea nameArea;

	SelectionsArea selectionsArea;

	private Layer layer;

	private SelectionLayers selectionLayers;

	public LayerSelections(SelectionLayers selectionLayers, Layer layer) {
		this.selectionLayers = selectionLayers;
		this.layer = layer;
		nameArea = new NameArea();
		selectionsArea = new SelectionsArea();
	}

	String computeOutputs() {
		int size = outputCount();
		if (size != 0) {
			return String.valueOf(size);
		}
		Layer firstLeaf = layer.firstLeaf();
		int firstLeafSize = selectionLayers.traversal.getSelections(firstLeaf)
				.size();
		if (firstLeafSize != 0) {
			return "-";
		} else {
			return "0";
		}
	}

	int outputCount() {
		return selectionLayers.traversal.getSelections(layer).size();
	}
}