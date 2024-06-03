package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.AnswerImpl;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TextTitle;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView.Ui;
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

	private int maxRenderedSelections = 250;

	public LayerSelections(SelectionLayers selectionLayers, Layer layer) {
		this.selectionLayers = selectionLayers;
		this.layer = layer;
		selectionsArea = new SelectionsArea();
		// after selectionsArea, since it requires the #selections rendered
		nameArea = new NameArea();
	}

	String computeOutputs() {
		int unfiliteredSelectionCount = unfiliteredSelectionCount();
		int filteredSelectionCount = selectionsArea.filtered.size();
		if (unfiliteredSelectionCount != 0) {
			if (filteredSelectionCount != unfiliteredSelectionCount
					&& filteredSelectionCount != maxRenderedSelections) {
				return Ax.format("%s/%s", filteredSelectionCount,
						unfiliteredSelectionCount);
			} else {
				return String.valueOf(unfiliteredSelectionCount);
			}
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

	int unfiliteredSelectionCount() {
		return selectionLayers.traversal.getSelections(layer).size();
	}

	@Directed(className = "bordered-area")
	class NameArea extends Model.All {
		@Directed
		LeafModel.TextTitle key;

		@Binding(type = Type.PROPERTY)
		boolean hasFilter;

		@Directed
		Filter filter;

		@Directed.Exclude
		String outputs;

		NameArea() {
			FormatBuilder keyBuilder = new FormatBuilder();
			keyBuilder.indent(layer.depth());
			keyBuilder.append(layer.getName());
			String keyString = keyBuilder.toString();
			outputs = computeOutputs();
			key = new TextTitle(keyString,
					Ax.format("%s : %s", keyString, outputs));
			filter = new Filter();
			hasFilter = Ax.notBlank(filter.existing);
		}

		class Filter extends Model.All implements DomEvents.Click.Handler {
			// TODO - dirndl - maybe a lightweight singleton Action? Although
			// this works well enough
			Link button;

			String existing;

			Filter() {
				button = Link.of(ModelEvents.Filter.class).withoutHref(true)
						.withClassName("filter").withText("");
				StandardLayerAttributes.Filter attr = Ui.place()
						.ensureAttributes(layer.index)
						.get(StandardLayerAttributes.Filter.class);
				if (attr != null) {
					this.existing = attr.toString();
				}
			}

			@Override
			public void onClick(Click event) {
				WidgetUtils.squelchCurrentEvent();
				FilterSuggestor suggestor = new FilterSuggestor();
				Overlay overlay = Overlay.builder()
						.dropdown(Position.START,
								provideElement().getBoundingClientRect(), this,
								new FilterSuggestor())
						.build();
				overlay.open();
			}

			@Directed.Delegating
			class FilterSuggestor extends Model.All
					implements ModelEvents.SelectionChanged.Handler {
				Suggestor suggestor;

				FilterSuggestor() {
					Suggestor.Attributes attributes = Suggestor.attributes();
					attributes.withFocusOnBind(true);
					attributes.withSuggestionXAlign(Position.CENTER);
					attributes.withLogicalAncestors(
							List.of(FilterSuggestor.class));
					TraversalPlace fromPlace = Ui.place()
							.truncateTo(layer.index);
					attributes.withAnswer(new AnswerImpl(
							Ui.get().createAnswerSupplier(fromPlace)));
					attributes.withNonOverlaySuggestionResults(true);
					attributes.withInputPrompt("Filter layer");
					suggestor = attributes.create();
				}

				@Override
				public void onSelectionChanged(SelectionChanged event) {
					throw new UnsupportedOperationException(
							"Unimplemented method 'onSelectionChanged'");
				}
			}
		}
	}

	class SelectionsArea extends Model.Fields {
		@Directed
		List<Object> selections;

		boolean parallel;

		LooseContextInstance snapshot;

		List<SelectionArea> filtered;

		SelectionsArea() {
			Stream<Selection> stream = selectionLayers.traversal
					.getSelections(layer).stream();
			parallel = Configuration.is(LayerSelections.class, "parallelTest");
			snapshot = LooseContext.getContext().snapshot();
			if (parallel) {
				stream.parallel();
			}
			testHistory.beforeFilter();
			List<Selection> filteredSelections = stream.filter(this::test)
					.limit(maxRenderedSelections).collect(Collectors.toList());
			boolean sortSelectedFirst = Ui.place()
					.attributesOrEmpty(layer.index)
					.has(StandardLayerAttributes.SortSelectedFirst.class);
			if (sortSelectedFirst) {
				Selection selection = Ui.place()
						.provideSelection(SelectionType.VIEW);
				Selection inSelectionPath = filteredSelections.stream()
						.filter(s -> s.isSelfOrAncestor(selection, false))
						.findFirst().orElse(null);
				if (inSelectionPath != null) {
					filteredSelections.remove(inSelectionPath);
					filteredSelections.add(0, inSelectionPath);
				}
			}
			filtered = filteredSelections.stream().map(SelectionArea::new)
					.toList();
			empty = filtered.isEmpty();
			selections = filtered.stream().collect(Collectors.toList());
			for (int idx = selections
					.size(); idx < maxRenderedSelections; idx++) {
				selections.add(new Spacer());
			}
			bindings().from(selectionLayers.page).on(Page.Property.place)
					.signal(this::updateSelected);
		}

		void updateSelected() {
			filtered.forEach(SelectionArea::updateSelected);
		}

		/*
		 * Cache test results for a given selection/string
		 */
		class TestHistory {
			String testFilter = "";

			Map<Selection, Boolean> results = new ConcurrentHashMap<>();

			boolean checkForExistingTest;

			void beforeFilter() {
				String incomingFilter = Page.traversalPlace().getTextFilter();
				checkForExistingTest = Objects.equals(testFilter,
						incomingFilter);
				if (!checkForExistingTest) {
					testFilter = incomingFilter;
					results.clear();
				}
			}

			Boolean getExistingResult(Selection selection) {
				return checkForExistingTest ? results.get(selection) : null;
			}

			void put(Selection selection, Boolean result) {
				results.put(selection, result);
			}
		}

		@Directed.Exclude
		TestHistory testHistory = new TestHistory();

		boolean test(Selection selection) {
			Boolean existingResult = testHistory.getExistingResult(selection);
			if (existingResult != null) {
				return existingResult;
			}
			Boolean result = null;
			if (parallel) {
				try {
					LooseContext.push();
					LooseContext.putSnapshotProperties(snapshot);
					result = Page.traversalPlace().test(selection);
				} finally {
					LooseContext.pop();
				}
			} else {
				result = Page.traversalPlace().test(selection);
			}
			testHistory.put(selection, result);
			return result;
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

			@Binding(type = Type.PROPERTY)
			boolean secondaryDescendantRelation;

			@Binding(type = Type.PROPERTY)
			boolean selected;

			@Binding(type = Type.PROPERTY)
			boolean ancestorOfSelected;

			public void setAncestorOfSelected(boolean ancestorOfSelected) {
				set("ancestorOfSelected", this.ancestorOfSelected,
						ancestorOfSelected,
						() -> this.ancestorOfSelected = ancestorOfSelected);
			}

			void setSelected(boolean selected) {
				set("selected", this.selected, selected,
						() -> this.selected = selected);
			}

			SelectionArea(Selection selection) {
				this.selection = selection;
				View view = selection.view();
				pathSegment = view.getPathSegment(selection);
				text = view.getText(selection);
				text = text == null ? "[gc]" : Ax.ntrim(Ax.trim(text, 100));
				selectionType = Page.traversalPlace().selectionType(selection);
				secondaryDescendantRelation = Page.traversalPlace()
						.isSecondaryDescendantRelation(selection);
				updateSelected();
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
				if (TraversalProcessView.Ui.get().isUseSelectionSegmentPath()) {
					selectionPath.segmentPath = selection.fullPath();
				}
				selectionPath.type = selectionType;
				event.reemitAs(this, TraversalEvents.SelectionSelected.class,
						selectionPath);
			}

			void updateSelected() {
				setSelected(Page.traversalPlace().isSelected(selection));
				setAncestorOfSelected(
						Page.traversalPlace().isAncestorOfSelected(selection));
			}
		}
	}

	static class Spacer extends Model {
	}
}