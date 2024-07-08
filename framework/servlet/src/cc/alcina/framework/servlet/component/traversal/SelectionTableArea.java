package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ref;
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
	public interface HasTableRepresentation {
		List<? extends Bindable> getSelectionBindables();

		/*
		 * returns the current selection children as table-viewables if they are
		 * all of the same (Bindable) type
		 */
		public interface Children extends HasTableRepresentation {
			@Override
			default List<? extends Bindable> getSelectionBindables() {
				Selection selection = (Selection) this;
				List list = selection.processNode().getChildren().stream()
						.map(pn -> ((Selection) pn.getValue()).get())
						.collect(Collectors.toList());
				Ref<Class> sameTypeCheck = Ref.empty();
				if (list.stream().allMatch(o -> {
					if (!(o instanceof Bindable)) {
						return false;
					}
					if (sameTypeCheck.isEmpty()) {
						sameTypeCheck.set(o.getClass());
					} else {
						if (sameTypeCheck.get() != o.getClass()) {
							return false;
						}
					}
					return true;
				})) {
					return list;
				} else {
					return null;
				}
			}

			@Override
			default Selection selectionFor(Object value) {
				Selection selection = (Selection) this;
				return selection.processNode().getChildren().stream()
						.map(pn -> ((Selection) pn.getValue()))
						.filter(sel -> sel.get() == value).findFirst().get();
			}
		}

		Selection selectionFor(Object object);
	}

	@Directed.Transform(TableView.class)
	@BeanViewModifiers(detached = true, nodeEditors = true)
	@DirectedContextResolver(DisplayAllMixin.class)
	List<? extends Bindable> selectionBindables;

	SelectionTableArea.HasTableRepresentation hasTable;

	public SelectionTableArea(Selection<?> selection) {
		hasTable = (SelectionTableArea.HasTableRepresentation) selection;
		selectionBindables = hasTable.getSelectionBindables();
	}

	@Override
	public void onRowClicked(TableEvents.RowClicked event) {
		Ui.place()
				.appendSelections(List.of(hasTable
						.selectionFor(event.getModel().getOriginalRowModel())))
				.go();
	}
}
