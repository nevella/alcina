package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.View;
import cc.alcina.framework.common.client.traversal.Selection.ViewAsync;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestionEntry;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.AnswerImpl;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.SuggestionSelected;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TextTitle;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
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

	private int maxRenderedSelections = Configuration
			.getInt("maxRenderedSelections");

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
	class NameArea extends Model.All implements ModelEvents.Closed.Handler {
		@Directed
		LeafModel.TextTitle key;

		@Binding(type = Type.PROPERTY)
		boolean hasFilter;

		@Binding(type = Type.PROPERTY)
		boolean filterEditorOpen;

		public void setFilterEditorOpen(boolean filterEditorOpen) {
			set("filterEditorOpen", this.filterEditorOpen, filterEditorOpen,
					() -> this.filterEditorOpen = filterEditorOpen);
		}

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
			hasFilter = filter.existing != null;
		}

		class Filter extends Model.All implements DomEvents.Click.Handler {
			// TODO - dirndl - maybe a lightweight singleton Action? Although
			// this works well enough
			Link button;

			@Directed(tag = "existing")
			LeafModel.TextTitle existing;

			private Overlay overlay;

			Filter() {
				button = Link.of(ModelEvents.Filter.class).withoutHref(true)
						.withClassName("filter").withText("");
				StandardLayerAttributes.Filter attr = Ui.place()
						.ensureAttributes(layer.index)
						.get(StandardLayerAttributes.Filter.class);
				if (attr != null) {
					this.existing = new TextTitle(attr.toString());
				}
			}

			@Override
			public void onClick(Click event) {
				WidgetUtils.squelchCurrentEvent();
				FilterSuggestor suggestor = new FilterSuggestor();
				overlay = Overlay.builder()
						.dropdown(Position.START,
								provideElement().getBoundingClientRect(), this,
								new FilterSuggestor())
						.build();
				setFilterEditorOpen(true);
				overlay.open();
			}

			@Directed.Delegating
			@DirectedContextResolver(AppSuggestor.Resolver.class)
			class FilterSuggestor extends Model.All
					implements ModelEvents.SelectionChanged.Handler {
				Suggestor suggestor;

				FilterSuggestor() {
					Suggestor.Attributes attributes = Suggestor.attributes();
					attributes.withFocusOnBind(true);
					attributes.withSelectAllOnFocus(true);
					attributes.withSuggestionXAlign(Position.CENTER);
					attributes.withLogicalAncestors(
							List.of(FilterSuggestor.class));
					attributes.withAnswer(new AnswerImpl(
							Ui.get().createAnswerSupplier(layer.index - 1)));
					attributes.withNonOverlaySuggestionResults(true);
					attributes.withInputPrompt("Filter layer");
					attributes.withInputExpandable(true);
					attributes.withInputText(
							filter.existing != null ? filter.existing.text
									: null);
					suggestor = attributes.create();
				}

				// copied from appsuggestor - that's ok, these really *are*
				// contextualised app suggestions
				@Override
				public void onSelectionChanged(SelectionChanged event) {
					AppSuggestionEntry suggestion = (AppSuggestionEntry) suggestor
							.provideSelectedValue();
					if (suggestion.url() != null) {
						History.newItem(suggestion.url());
					} else {
						event.reemitAs(this, suggestion.modelEvent(),
								suggestion.eventData());
					}
					suggestor.closeSuggestions();
					overlay.close(null, false);
					suggestor.setValue(null);
					event.reemitAs(this, SuggestionSelected.class);
				}
			}
		}

		@Override
		public void onClosed(Closed event) {
			setFilterEditorOpen(false);
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
					.collect(Collectors.toList());
			empty = filtered.isEmpty();
			selections = filtered.stream().collect(Collectors.toList());
			for (int idx = selections.size(); idx <
			// hardcoded, matches the css grid
					250; idx++) {
				selections.add(new Spacer());
			}
			bindings().from(Ui.get()).on(Ui.properties.place)
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
				String incomingFilter = Ui.place().getTextFilter();
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
					result = Ui.place().test(selection);
				} finally {
					LooseContext.pop();
				}
			} else {
				result = Ui.place().test(selection);
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
				if (view instanceof ViewAsync) {
					Client.eventBus().queued().lambda(this::render).deferred()
							.dispatch();
				} else {
					render();
				}
			}

			@Override
			public void onBind(Bind event) {
				super.onBind(event);
				if (event.isBound() && selected) {
					provideElement().scrollIntoView();
				}
			}

			void render() {
				View view = selection.view();
				pathSegment = view.getPathSegment(selection);
				text = view.getText(selection);
				text = text == null ? "[gc]" : Ax.ntrim(Ax.trim(text, 100));
				selectionType = Ui.place().selectionType(selection);
				secondaryDescendantRelation = Ui.place()
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
				if (TraversalBrowser.Ui.get().isUseSelectionSegmentPath()) {
					selectionPath.segmentPath = selection.fullPath();
				}
				selectionPath.type = selectionType;
				event.reemitAs(this, TraversalEvents.SelectionSelected.class,
						selectionPath);
			}

			void updateSelected() {
				setSelected(Ui.place().isSelected(selection));
				setAncestorOfSelected(
						Ui.place().isAncestorOfSelected(selection));
			}
		}
	}

	static class Spacer extends Model {
	}
}