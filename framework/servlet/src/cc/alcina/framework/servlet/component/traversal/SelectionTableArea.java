package cc.alcina.framework.servlet.component.traversal;

import java.util.List;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells;
import cc.alcina.framework.gwt.client.dirndl.model.BeanViewModifiers;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.TableView;

/*
 * TODO - extend the heck out of this
 */
@DirectedContextResolver(FmsContentCells.FmsCellsContextResolver.class)
public class SelectionTableArea extends Model.Fields {
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
						.map(pn -> ((Selection) pn.getValue()).get()).toList();
				Ref<Class> sameTypeCheck = Ref.empty();
				if (list.stream().allMatch(o -> {
					if (o instanceof Bindable) {
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
		}
	}

	@Directed.Transform(TableView.class)
	@BeanViewModifiers(detached = true)
	List<? extends Bindable> selectionBindables;

	SelectionTableArea.HasTableRepresentation hasTable;

	public SelectionTableArea(Selection<?> selection) {
		hasTable = (SelectionTableArea.HasTableRepresentation) selection;
		selectionBindables = hasTable.getSelectionBindables();
	}
}
