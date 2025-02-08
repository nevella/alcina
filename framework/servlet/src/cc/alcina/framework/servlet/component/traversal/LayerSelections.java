package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.context.LooseContextInstance;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.View;
import cc.alcina.framework.common.client.traversal.Selection.ViewAsync;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceAdapter;
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
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
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
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;

@TypedProperties
class LayerSelections extends Model.All {
	static PackageProperties._LayerSelections properties = PackageProperties.layerSelections;

	@Binding(type = Type.PROPERTY)
	boolean empty;

	NameArea nameArea;

	SelectionsArea selectionsArea;

	@Property.Not
	Layer layer;

	private SelectionLayers selectionLayers;

	private int maxRenderedSelections = Configuration
			.getInt("maxRenderedSelections");

	public LayerSelections(SelectionLayers selectionLayers, Layer layer) {
		this.selectionLayers = selectionLayers;
		this.layer = layer;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LayerSelections) {
			LayerSelections o = (LayerSelections) obj;
			return HasEquivalence.areEquivalent(LayerEquivalence.class, layer,
					o.layer);
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		return new LayerEquivalence().withReferent(layer).equivalenceHash();
	}

	/*
	 * Models equivalence for rendering purposes
	 */
	static class LayerEquivalence
			extends HasEquivalenceAdapter<Layer, LayerEquivalence> {
		@Override
		public int equivalenceHash() {
			return Objects.hash(o.index, o.getClass(), o.getSelections());
		}

		@Override
		public boolean equivalentTo(LayerEquivalence other) {
			return CommonUtils.equals(o.index, other.o.index, o.getClass(),
					other.o.getClass(), o.getSelections(),
					other.o.getSelections());
		}
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		nameArea = new NameArea();
		selectionsArea = new SelectionsArea();
		super.onBeforeRender(event);
	}

	String computeOutputs() {
		int unfiliteredSelectionCount = unfilteredSelectionCount();
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
		int firstLeafSize = selectionLayers.traversal().getSelections(firstLeaf)
				.size();
		if (firstLeafSize != 0) {
			return "-";
		} else {
			return "0";
		}
	}

	int unfilteredSelectionCount() {
		return selectionLayers.traversal().getSelections(layer).size();
	}

	@Property.Not
	StandardLayerAttributes.Filter getLayerFilterAttribute() {
		return Ui.place().ensureAttributes(layer.index)
				.get(StandardLayerAttributes.Filter.class);
	}

	// FIXME - once inner classes allow static (JDK16) use typedproperties,
	// remvoe setters
	@Directed(className = "bordered")
	@TypedProperties
	class NameArea extends Model.All
			implements ModelEvents.Closed.Handler, DomEvents.Click.Handler {
		@Directed
		LeafModel.TextTitle key;

		NameArea() {
			bindings().from(selectionLayers.page.ui).on(Ui.properties.place)
					.map(this::computeSelected).to(this).on("selected")
					.oneWay();
		}

		@Binding(type = Type.PROPERTY)
		boolean hasFilter;

		@Binding(type = Type.PROPERTY)
		boolean filterEditorOpen;

		@Directed
		Filter filter;

		@Directed.Exclude
		String outputs;

		@Binding(type = Type.PROPERTY)
		boolean selected;

		@Override
		public void onBeforeRender(BeforeRender event) {
			bindings().from(LayerSelections.this.selectionsArea)
					.on(_SelectionsArea_properties.selections).nonNull()
					.signal(this::render);
			super.onBeforeRender(event);
		}

		void render() {
			Client.eventBus().queued().lambda(this::render0).dispatch();
		}

		void render0() {
			FormatBuilder keyBuilder = new FormatBuilder();
			keyBuilder.indent(layer.depth());
			keyBuilder.append(layer.getName());
			String keyString = keyBuilder.toString();
			outputs = computeOutputs();
			_NameArea_properties.key.set(this, new TextTitle(keyString,
					Ax.format("%s : %s", keyString, outputs)));
			_NameArea_properties.filter.set(this, new Filter());
			_NameArea_properties.hasFilter.set(this, filter.existing != null);
		}

		boolean computeSelected(Place place) {
			TraversalPlace traversalPlace = (TraversalPlace) place;
			return traversalPlace.isSelected(layer);
		}

		public void onClick(Click event) {
			event.reemitAs(this, TraversalEvents.LayerSelectionChange.class,
					layer);
		}

		class Filter extends Model.All implements ModelEvents.Filter.Handler {
			// TODO - dirndl - maybe a lightweight singleton Action? Although
			// this works well enough
			Link button;

			@Directed(
				tag = "existing",
				reemits = { DomEvents.Click.class, ModelEvents.Filter.class })
			LeafModel.TextTitle existing;

			private Overlay overlay;

			Filter() {
				button = Link.of(ModelEvents.Filter.class).withoutHref(true)
						.withClassName("filter").withText("");
				StandardLayerAttributes.Filter attr = getLayerFilterAttribute();
				if (attr != null) {
					this.existing = new TextTitle(attr.toString());
				}
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

			@Override
			public void onFilter(ModelEvents.Filter event) {
				WidgetUtils.squelchCurrentEvent();
				FilterSuggestor suggestor = new FilterSuggestor();
				overlay = Overlay.attributes()
						.dropdown(Position.START,
								provideElement().getBoundingClientRect(), this,
								new FilterSuggestor())
						.create();
				_NameArea_properties.filterEditorOpen.set(NameArea.this, true);
				overlay.open();
			}
		}

		@Override
		public void onClosed(Closed event) {
			_NameArea_properties.filterEditorOpen.set(NameArea.this, false);
		}
	}

	static PackageProperties._LayerSelections_SelectionsArea _SelectionsArea_properties = PackageProperties.layerSelections_selectionsArea;

	static PackageProperties._LayerSelections_NameArea _NameArea_properties = PackageProperties.layerSelections_nameArea;

	/*
	 * Models the (possibly filtered) selections in a leyer
	 */
	@TypedProperties
	class SelectionsArea extends Model.Fields {
		@Directed
		List<Object> selections;

		boolean parallel;

		LooseContextInstance snapshot;

		List<SelectionArea> filtered;

		List<Selection> filteredSelections;

		SelectionsArea() {
			bindings().from(Ui.get()).on(Ui.properties.place)
					.signal(this::update);
		}

		void update() {
			if (testHistory != null && testHistory.isCurrent()) {
				filtered.forEach(SelectionArea::updateSelected);
			} else {
				render();
			}
		}

		void render() {
			Stream<Selection> stream = selectionLayers.traversal()
					.getSelections(layer).stream();
			if (layer.getName().equals("VersionList.JsonLayer")) {
				int debug = 3;
			}
			parallel = Configuration.is(LayerSelections.class, "parallelTest");
			snapshot = LooseContext.getContext().snapshot();
			if (parallel) {
				stream.parallel();
			}
			testHistory.refreshTextFilter();
			filteredSelections = stream.filter(this::test)
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
			empty = filtered.isEmpty() && getLayerFilterAttribute() == null;
			List<Object> selections = filtered.stream()
					.collect(Collectors.toList());
			int start = selections.size();
			// hardcoded, matches the css grid
			int end = 250;
			if (start < end) {
				// offsets allow for name area, the fact that the line index is
				// +1 (one based) +1 (end of grid item)
				selections.add(new Spacer(start + 2, end + 2));
			}
			_SelectionsArea_properties.selections.set(this, selections);
		}

		/*
		 * Cache test results for a given selection/string
		 */
		class TestHistory {
			String textFilter = "";

			Map<Selection, Boolean> results = new ConcurrentHashMap<>();

			boolean checkForExistingTest;

			SelectionType firstSelectionType;

			void refreshTextFilter() {
				String textFilter = Ui.place().getTextFilter();
				SelectionType firstSelectionType = Ui.place()
						.firstSelectionType();
				if (!isCurrent()) {
					this.textFilter = textFilter;
					this.firstSelectionType = firstSelectionType;
					results.clear();
				}
			}

			boolean isCurrent() {
				String textFilter = Ui.place().getTextFilter();
				SelectionType firstSelectionType = Ui.place()
						.firstSelectionType();
				return CommonUtils.equals(textFilter, this.textFilter,
						firstSelectionType, this.firstSelectionType);
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

		@Directed(className = "bordered")
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

	static class Spacer extends Model.Fields {
		// @Binding(type = Type.STYLE_ATTRIBUTE)
		int gridColumnStart;

		// @Binding(type = Type.STYLE_ATTRIBUTE)
		int gridColumnEnd;

		// chrome bug -
		// https://stackoverflow.com/questions/74935509/why-does-chrome-devtools-complain-that-i-should-not-use-grid-column-end-on-an-el
		@Binding(type = Type.STYLE_ATTRIBUTE)
		String gridColumn;

		Spacer(int start, int end) {
			this.gridColumnStart = start;
			this.gridColumnEnd = end;
			gridColumn = Ax.format("%s/%s", start, end);
		}
	}
}