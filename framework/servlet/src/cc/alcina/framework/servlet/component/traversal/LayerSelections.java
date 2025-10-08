package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.place.shared.Place;

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
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TextTitle;
import cc.alcina.framework.gwt.client.dirndl.model.IfNotEqual;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.LayerSelections.SelectionsArea;
import cc.alcina.framework.servlet.component.traversal.PackageProperties._LayerSelections_SelectionsArea_SelectionArea.InstanceProperties;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes.Filter;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;

/**
 * A row in the ui - a {@link NameArea} modelling the metadata of the layer, a
 * {@link SelectionsArea} containing multiple
 * {@link SelectionsArea.SelectionArea} cells
 */
@TypedProperties
class LayerSelections extends Model.All implements IfNotEqual {
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

	// FIXME - once inner classes allow static (JDK16) use typedproperties,
	// remvoe setters
	@Directed(className = "bordered")
	@TypedProperties
	class NameArea extends Model.All implements ModelEvents.Closed.Handler,
			DomEvents.Click.Handler, LayerFilterEditor.Host {
		@Directed
		LeafModel.TextTitle key;

		@Binding(type = Type.PROPERTY)
		boolean hasFilter;

		@Binding(type = Type.PROPERTY)
		boolean filterEditorOpen;

		@Directed
		LayerFilterEditor filter;

		@Directed.Exclude
		String outputs;

		@Binding(type = Type.PROPERTY)
		boolean selected;

		NameArea() {
			bindings().from(selectionLayers.page.ui).on(Ui.properties.place)
					.map(this::computeSelected).to(this).on("selected")
					.oneWay();
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			bindings().from(LayerSelections.this.selectionsArea)
					.on(_SelectionsArea_properties.selections).nonNull()
					.signal(this::render);
			super.onBeforeRender(event);
		}

		public void onClick(Click event) {
			event.reemitAs(this, TraversalEvents.LayerSelectionChange.class,
					layer);
		}

		@Override
		public void onClosed(Closed event) {
			_NameArea_properties.filterEditorOpen.set(NameArea.this, false);
		}

		@Property.Not
		@Override
		public Filter getLayerFilterAttribute() {
			return LayerSelections.this.getLayerFilterAttribute();
		}

		@Property.Not
		@Override
		public Layer getLayer() {
			return layer;
		}

		@Override
		public void setFilterEditorOpen(boolean filterEditorOpen) {
			set("filterEditorOpen", this.filterEditorOpen, filterEditorOpen,
					() -> this.filterEditorOpen = filterEditorOpen);
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
			_NameArea_properties.filter.set(this, new LayerFilterEditor(this));
			_NameArea_properties.hasFilter.set(this, filter.existing != null);
		}

		boolean computeSelected(Place place) {
			TraversalPlace traversalPlace = (TraversalPlace) place;
			return traversalPlace.isSelected(layer);
		}
	}

	/*
	 * Models the (possibly filtered) selections in a leyer
	 */
	@TypedProperties
	class SelectionsArea extends Model.Fields {
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

		@TypedProperties
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

			PackageProperties._LayerSelections_SelectionsArea_SelectionArea.InstanceProperties
					properties() {
				return PackageProperties.layerSelections_selectionsArea_selectionArea
						.instance(this);
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

			@Override
			public void onClick(Click event) {
				SelectionPath selectionPath = TraversalBrowser.Ui.get()
						.getSelectionPath(selection);
				event.reemitAs(this, TraversalEvents.SelectionSelected.class,
						selectionPath);
			}

			void render() {
				View view = selection.view();
				InstanceProperties properties = properties();
				properties.pathSegment().set(view.getPathSegment(selection));
				String text = view.getText(selection);
				text = text == null ? "[gc]" : Ax.ntrim(Ax.trim(text, 100));
				properties.text().set(text);
				properties.selectionType()
						.set(Ui.place().selectionType(selection));
				properties.secondaryDescendantRelation().set(
						Ui.place().isSecondaryDescendantRelation(selection));
				updateSelected();
			}

			void updateSelected() {
				properties().selected().set(Ui.place().isSelected(selection));
				properties().ancestorOfSelected()
						.set(Ui.place().isAncestorOfSelected(selection));
			}
		}

		@Directed
		List<Object> selections;

		boolean parallel;

		LooseContextInstance snapshot;

		List<SelectionArea> filtered;

		List<Selection> filteredSelections;

		List<Selection> tableSelections;

		@Directed.Exclude
		TestHistory testHistory = new TestHistory();

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
			Stream<Selection> stream = selectionLayers.traversal().selections()
					.byLayer(layer).stream();
			parallel = Configuration.is(LayerSelections.class, "parallelTest");
			snapshot = LooseContext.getContext().snapshot();
			if (parallel) {
				stream.parallel();
			}
			testHistory.refreshTextFilter();
			int maxSelections = Math.max(maxRenderedSelections,
					TraversalSettings.get().tableRows);
			tableSelections = stream.filter(this::test).limit(maxSelections)
					.collect(Collectors.toList());
			filteredSelections = tableSelections.stream()
					.limit(maxRenderedSelections).collect(Collectors.toList());
			boolean sortSelectedFirst = Ui.place()
					.attributesOrEmpty(layer.index)
					.has(StandardLayerAttributes.SortSelectedFirst.class);
			if (sortSelectedFirst) {
				Selection selection = Ui.place()
						.provideSelection(SelectionType.VIEW);
				Selection inSelectionPath = tableSelections.stream()
						.filter(s -> s.isSelfOrAncestor(selection, false))
						.findFirst().orElse(null);
				if (inSelectionPath != null) {
					filteredSelections.remove(inSelectionPath);
					filteredSelections.add(0, inSelectionPath);
					tableSelections.remove(inSelectionPath);
					tableSelections.add(0, inSelectionPath);
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

	static PackageProperties._LayerSelections properties = PackageProperties.layerSelections;

	static PackageProperties._LayerSelections_SelectionsArea _SelectionsArea_properties = PackageProperties.layerSelections_selectionsArea;

	static PackageProperties._LayerSelections_NameArea _NameArea_properties = PackageProperties.layerSelections_nameArea;

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
		int firstLeafSize = selectionLayers.traversal().selections()
				.byLayer(firstLeaf).size();
		if (firstLeafSize != 0) {
			return "-";
		} else {
			return "0";
		}
	}

	int unfilteredSelectionCount() {
		return selectionLayers.traversal().selections().byLayer(layer).size();
	}

	@Property.Not
	StandardLayerAttributes.Filter getLayerFilterAttribute() {
		return Ui.activePlace().ensureAttributes(layer.index)
				.get(StandardLayerAttributes.Filter.class);
	}
}