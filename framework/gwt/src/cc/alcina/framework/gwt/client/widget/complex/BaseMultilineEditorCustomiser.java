package cc.alcina.framework.gwt.client.widget.complex;

import java.util.List;
import java.util.function.Predicate;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.widget.Link;

public abstract class BaseMultilineEditorCustomiser<T extends Entity>
		implements Customiser, BoundWidgetProvider {
	private boolean editable;

	protected boolean asMultipleGrids() {
		return false;
	}

	public void customiseActions(List<PermissibleAction> actions) {
	}

	public void customiseContentViewFactory(
			ContentViewFactory contentViewFactory, Object model) {
	}

	public List<Link> customisePerRowEditActions(List<Link> actions, T rowValue,
			BaseMultilineEditor editor) {
		return actions;
	}

	protected abstract void deleteItem(T t);

	public abstract void doCreateRow(Object model,
			BaseMultilineEditor<T> editor);

	public void doDeleteRows(List<T> items, BaseMultilineEditor<T> editor) {
		for (T t : items) {
			deleteItem(t);
			editor.getValue().remove(t);
		}
		editor.redraw();
	}

	public List<T> filterVisibleValues(List<T> values) {
		return values;
	}

	@Override
	public BoundWidget get() {
		BaseMultilineEditor editor = asMultipleGrids()
				? new BaseMultilineGridEditor()
				: new BaseMultilineRowEditor();
		editor.setCustomiser(this);
		editor.setEditable(editable);
		return (BoundWidget) editor;
	}

	public abstract String getCreateActionDisplayName();

	protected Predicate<Field> getFieldFilter() {
		return field -> {
			switch (field.getPropertyName()) {
			case "note":
			case "creationContact":
			case "creationDate":
				return true;
			default:
				return false;
			}
		};
	}

	public abstract Class<T> getItemClass();

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom params) {
		this.editable = editable;
		return this;
	}

	public boolean handleCustomAction(BaseMultilineEditor<T> editor,
			PermissibleAction action) {
		return false;
	}

	public void sortValues(List<T> values) {
		values.sort(Entity.EntityComparatorLocalsHigh.INSTANCE);
	}
}
