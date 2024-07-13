package cc.alcina.framework.servlet.component.sequence;

import java.util.List;

import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells.FmsCellsContextResolver;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells.FmsCellsContextResolver.DisplayAllMixin;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.BeanViewModifiers;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.RowClicked;
import cc.alcina.framework.gwt.client.dirndl.model.TableEvents.RowsModelAttached;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.RowsModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableView;

@Directed(tag = "sequence")
// this is just to force the SequenceArea to be accessible from RowTransformer
// (via the resolver)
@DirectedContextResolver(FmsCellsContextResolver.class)
class SequenceArea extends Model.Fields
		implements TableEvents.RowsModelAttached.Handler {
	@Directed
	Heading header;

	@Directed.Transform(TableView.class)
	@TableModel.RowTransformer(RowTransformer.class)
	@BeanViewModifiers(detached = true, nodeEditors = true)
	@DirectedContextResolver(DisplayAllMixin.class)
	List<?> filteredElements;

	Page page;

	static class RowTransformer
			extends ModelTransform.AbstractContextSensitiveModelTransform {
		private ModelTransform sequenceRowTransform;

		@Override
		public AbstractContextSensitiveModelTransform
				withContextNode(Node node) {
			SequenceArea ctx = node.getResolver().parent().getRootModel();
			sequenceRowTransform = ctx.page.sequence.getRowTransform();
			return super.withContextNode(node);
		}

		@Override
		public Object apply(Object t) {
			return sequenceRowTransform.apply(t);
		}
	}

	SequenceArea(Page page) {
		header = new Heading("Sequence elements");
		this.page = page;
		filteredElements = page.filteredSequenceElements;
	}

	public void onRowClicked(RowClicked event) {
		Object rowModel = event.getModel().getOriginalRowModel();
		int index = page.sequence.getElements().indexOf(rowModel);
		if (event.getContext().getOriginatingNativeEvent().getShiftKey()) {
			if (page.ui.place.selectedElementIdx != -1) {
				IntPair absolutePair = IntPair
						.of(page.ui.place.selectedElementIdx, index)
						.toLowestFirst();
				page.ui.place.copy().withSelectedRange(absolutePair).go();
			}
		} else {
			page.ui.place.copy().withSelectedElementIdx(index).go();
		}
	}

	RowsModel rowsModel;

	@Override
	public void onRowsModelAttached(RowsModelAttached event) {
		this.rowsModel = event.getModel();
		this.rowsModel.topicSelectedRowsChanged
				.add(this::onSelectedRowsChanged);
		page.highlightModel.elementMatches.keySet().forEach(sequenceElement -> {
			int visibleRowIndex = filteredElements.indexOf(sequenceElement);
			if (visibleRowIndex != -1) {
				rowsModel.meta.get(visibleRowIndex).setFlag("matches", true);
			}
		});
		if (page.ui.place.selectedElementIdx != -1) {
			Object selectedElement = page.sequence.getElements()
					.get(page.ui.place.selectedElementIdx);
			int visibleRowIndex = filteredElements.indexOf(selectedElement);
			rowsModel.select(visibleRowIndex);
			rowsModel.scrollSelectedIntoView();
		}
	}

	void onSelectedRowsChanged() {
		IntPair selected = rowsModel.getSelectedRowsRange();
		if (selected != null) {
			if (selected.isPoint()) {
				page.ui.place.copy().withSelectedElementIdx(selected.i1).go();
			} else {
				page.ui.place.copy().withSelectedElementIdx(selected.i1)
						.withSelectedRange(selected).go();
			}
		}
	}
}
