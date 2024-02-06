package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DomEvent;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.View;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

class LayerSelections extends Model.All {
	@Binding(type = Type.PROPERTY)
	boolean empty;

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

	class SelectionsArea extends Model.All {
		List<Object> selections;

		private boolean parallel;

		private LooseContextInstance snapshot;

		SelectionsArea() {
			Stream<Selection> stream = selectionLayers.traversal
					.getSelections(layer).stream();
			parallel = Configuration.is(LayerSelections.class, "parallelTest");
			snapshot = LooseContext.getContext().snapshot();
			if (parallel) {
				stream.parallel();
			}
			List<Selection> filtered = stream.filter(this::test).limit(5)
					.toList();
			selections = filtered.stream().map(SelectionArea::new)
					.collect(Collectors.toList());
			empty = selections.isEmpty();
			for (int idx = selections.size(); idx < 5; idx++) {
				selections.add(new Spacer());
			}
		}

		boolean test(Selection selection) {
			if (parallel) {
				try {
					LooseContext.push();
					LooseContext.putSnapshotProperties(snapshot);
					return Page.traversalPlace().test(selection);
				} finally {
					LooseContext.pop();
				}
			} else {
				return Page.traversalPlace().test(selection);
			}
		}

		@Directed(className = "bordered-area")
		class SelectionArea extends Model.All
				implements DomEvents.Click.Handler {
			String pathSegment;

			String type;

			String text;

			private Selection selection;

			@Binding(type = Type.PROPERTY)
			TraversalPlace.SelectionType selectionType;

			SelectionArea(Selection selection) {
				this.selection = selection;
				View view = selection.view();
				pathSegment = view.getPathSegment(selection);
				text = view.getText(selection);
				text = text == null ? "[gc]" : Ax.ntrim(Ax.trim(text, 100));
				selectionType = Page.traversalPlace().selectionType(selection);
			}

			@Override
			public void onClick(Click event) {
				DomEvent domEvent = (DomEvent) event.getContext()
						.getOriginatingGwtEvent();
				NativeEvent nativeEvent = domEvent.getNativeEvent();
				TraversalPlace.SelectionType selectionType = SelectionType.VIEW;
				SelectionPath selectionPath = new TraversalPlace.SelectionPath();
				selectionPath.selection = selection;
				selectionPath.path = selection.processNode().treePath();
				selectionPath.type = selectionType;
				event.reemitAs(this, TraversalEvents.SelectionSelected.class,
						selectionPath);
			}
		}
	}

	static class Spacer extends Model {
	}
}