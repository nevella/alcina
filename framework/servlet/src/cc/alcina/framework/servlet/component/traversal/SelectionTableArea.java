package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.RowView;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells.FmsCellsContextResolver.DisplayAllMixin;
import cc.alcina.framework.gwt.client.dirndl.model.BeanViewModifiers;
import cc.alcina.framework.gwt.client.dirndl.model.IfNotExisting;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents;
import cc.alcina.framework.gwt.client.dirndl.model.TableView;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;

/*
 * TODO - extend the heck out of this
 */
@DirectedContextResolver(FmsContentCells.FmsCellsContextResolver.class)
@TypeSerialization(reflectiveSerializable = false)
public class SelectionTableArea extends Model.Fields
		implements TableEvents.RowClicked.Handler, IfNotExisting {
	@Directed.Transform(TableView.class)
	@BeanViewModifiers(detached = true, nodeEditors = true)
	@DirectedContextResolver(DisplayAllMixin.class)
	List<? extends Bindable> selectionBindables;

	Selection.HasTableRepresentation hasTable;

	TraversalPlace appendRowSelectionTo;

	Layer selectionLayer;

	public SelectionTableArea(Layer layer, Selection<?> selection) {
		hasTable = (Selection.HasTableRepresentation) selection;
		selectionLayer = layer;
		selectionBindables = hasTable.getSelectionBindables();
		appendRowSelectionTo = Ui.place();
	}

	public SelectionTableArea(Layer layer,
			List<? extends Selection> filteredLayerSelections) {
		selectionLayer = layer;
		hasTable = new LayerToTable(layer, filteredLayerSelections);
		selectionBindables = hasTable.getSelectionBindables();
		appendRowSelectionTo = Ui.place().truncateTo(layer.index);
	}

	class LayerToTable
			implements Selection.HasTableRepresentation, IfNotExisting {
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
			return ((RowView) object).getSelection();
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
}
