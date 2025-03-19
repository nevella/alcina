package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.RowView;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.EmitDescent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells.FmsCellsContextResolver.DisplayAllMixin;
import cc.alcina.framework.gwt.client.dirndl.model.BeanViewModifiers;
import cc.alcina.framework.gwt.client.dirndl.model.IfNotEqual;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableColumnMetadata;
import cc.alcina.framework.gwt.client.dirndl.model.TableColumnMetadata.EditFilter;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents;
import cc.alcina.framework.gwt.client.dirndl.model.TableView;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.servlet.component.traversal.LayerFilterEditor.FilterSuggestor;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes.Filter;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;

/*
 * TODO - extend the heck out of this
 */
@DirectedContextResolver(FmsContentCells.FmsCellsContextResolver.class)
@TypeSerialization(reflectiveSerializable = false)
public class SelectionTableArea extends Model.Fields
		implements TableEvents.RowClicked.Handler, IfNotEqual,
		TableColumnMetadata.Change.Emitter,
		TableColumnMetadata.EditFilter.Handler,
		LayoutEvents.EmitDescent.Handler {
	@Directed.Transform(TableView.class)
	@BeanViewModifiers(detached = true, nodeEditors = true)
	@DirectedContextResolver(DisplayAllMixin.class)
	List<? extends Bindable> selectionBindables;

	Selection.HasTableRepresentation hasTable;

	TraversalPlace appendRowSelectionTo;

	Layer selectionLayer;

	Layer filterLayer;

	private FilterHostImpl openFilter;

	public SelectionTableArea(Layer layer, Selection<?> selection) {
		hasTable = (Selection.HasTableRepresentation) selection;
		selectionLayer = layer;
		filterLayer = Ui.traversal().getLayer(selectionLayer.index + 1);
		selectionBindables = hasTable.getSelectionBindables();
		appendRowSelectionTo = Ui.place();
	}

	public SelectionTableArea(Layer layer,
			List<? extends Selection> filteredLayerSelections) {
		selectionLayer = layer;
		filterLayer = selectionLayer;
		hasTable = new LayerToTable(layer, filteredLayerSelections);
		selectionBindables = hasTable.getSelectionBindables();
		appendRowSelectionTo = Ui.place().truncateTo(layer.index);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			emitEvent(TraversalEvents.SelectionTableAreaChange.class,
					selectionBindables);
		}
	}

	class LayerToTable implements Selection.HasTableRepresentation, IfNotEqual {
		Layer layer;

		List<? extends Selection> filteredLayerSelections;

		LayerToTable(Layer layer,
				List<? extends Selection> filteredLayerSelections) {
			this.layer = layer;
			this.filteredLayerSelections = filteredLayerSelections;
		}

		@Override
		public List<? extends Bindable> getSelectionBindables() {
			return (List) filteredLayerSelections.stream().map(sel -> {
				RowView rowView = ((Selection) sel).rowView();
				return rowView == null ? null : rowView.provideBindable();
			}).filter(Objects::nonNull).collect(Collectors.toList());
		}

		@Override
		public boolean equals(Object input) {
			if (input instanceof LayerToTable) {
				LayerToTable typed = (LayerToTable) input;
				return Objects.equals(typed.layer, layer);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return 1;// force equals comparison
		}

		@Override
		public Selection selectionFor(Object object) {
			return filteredLayerSelections.stream()
					.filter(sel -> sel.get() == object).findFirst()
					.orElse(null);
		}
	}

	@Override
	public void onRowClicked(TableEvents.RowClicked event) {
		Selection selection = hasTable
				.selectionFor(event.getModel().getOriginalRowModel());
		if (Ui.get().isAppendTableSelections()) {
			appendRowSelectionTo.appendSelections(List.of(selection)).go();
		} else {
			TraversalPlace.SelectionType selectionType = SelectionType.VIEW;
			SelectionPath selectionPath = new TraversalPlace.SelectionPath();
			selectionPath.selection = selection;
			selectionPath.path = selection.processNode().treePath();
			selectionPath.type = selectionType;
			event.reemitAs(this, TraversalEvents.SelectionSelected.class,
					selectionPath);
		}
	}

	@Override
	public boolean equals(Object input) {
		if (input instanceof SelectionTableArea) {
			SelectionTableArea typed = (SelectionTableArea) input;
			return typed != null && Objects.equals(typed.hasTable, hasTable);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return 1;// force equals comparison
	}

	class TableColumnMetadataImpl implements TableColumnMetadata {
		@Override
		public ColumnMetadata getColumnMetadata(Property property) {
			ColumnMetadata columnMetadata = Ui.place()
					.getColumnMetadata(selectionLayer, property);
			if (openFilter != null && openFilter.property == property) {
				columnMetadata.setFilterOpen(true);
			}
			return columnMetadata;
		}
	}

	@Override
	public void onEmitDescent(EmitDescent event) {
		emitColumnMetadata();
	}

	void emitColumnMetadata() {
		emitEvent(TableColumnMetadata.Change.class,
				new TableColumnMetadataImpl());
	}

	@Override
	public void onEditFilter(EditFilter event) {
		openFilter = new FilterHostImpl(event);
		openFilter.open();
		emitColumnMetadata();
	}

	public void onFilterClosed() {
		openFilter = null;
		emitColumnMetadata();
	}

	class FilterHostImpl
			implements LayerFilterEditor.Host, ModelEvents.Closed.Handler {
		Property property;

		Element relativeTo;

		Overlay overlay;

		FilterHostImpl(EditFilter event) {
			this.property = event.getModel().provideProperty();
			this.relativeTo = ((Model) event.getModel()).provideElement();
			FilterSuggestor suggestor = new FilterSuggestor(this);
			overlay = Overlay.attributes()
					.dropdown(Position.START,
							relativeTo.getBoundingClientRect(),
							SelectionTableArea.this, suggestor)
					.withClosedHandler(this).create();
		}

		void open() {
			overlay.open();
		}

		@Override
		public Filter getLayerFilterAttribute() {
			return Ui.place().ensureAttributes(selectionLayer.index)
					.get(StandardLayerAttributes.Filter.class);
		}

		@Override
		public Layer getLayer() {
			return filterLayer;
		}

		@Override
		public void setFilterEditorOpen(boolean open) {
			// we're causing this imperatively
		}

		@Override
		public void onClosed(Closed event) {
			onFilterClosed();
		}
	}
}
