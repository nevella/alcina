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
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents;
import cc.alcina.framework.gwt.client.dirndl.model.TableView;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;

/*
 * TODO - extend the heck out of this
 */
@DirectedContextResolver(FmsContentCells.FmsCellsContextResolver.class)
@TypeSerialization(reflectiveSerializable = false)
public class SelectionTableArea extends Model.Fields
		implements TableEvents.RowClicked.Handler {
	@Directed.Transform(TableView.class)
	@BeanViewModifiers(detached = true, nodeEditors = true)
	@DirectedContextResolver(DisplayAllMixin.class)
	List<? extends Bindable> selectionBindables;

	Selection.HasTableRepresentation hasTable;

	public SelectionTableArea(Selection<?> selection) {
		hasTable = (Selection.HasTableRepresentation) selection;
		selectionBindables = hasTable.getSelectionBindables();
	}

	public SelectionTableArea(Layer layer) {
		hasTable = new LayerToTable(layer);
		selectionBindables = hasTable.getSelectionBindables();
	}

	class LayerToTable implements Selection.HasTableRepresentation {
		Layer layer;

		LayerToTable(Layer layer) {
			this.layer = layer;
		}

		@Override
		public List<? extends Bindable> getSelectionBindables() {
			return (List) layer.getSelections().stream().map(sel -> {
				RowView rowView = ((Selection) sel).rowView();
				return rowView == null ? null : rowView.provideBindable();
			}).filter(Objects::nonNull).collect(Collectors.toList());
		}

		@Override
		public Selection selectionFor(Object object) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'selectionFor'");
		}
	}

	@Override
	public void onRowClicked(TableEvents.RowClicked event) {
		Ui.place()
				.appendSelections(List.of(hasTable
						.selectionFor(event.getModel().getOriginalRowModel())))
				.go();
	}
}
