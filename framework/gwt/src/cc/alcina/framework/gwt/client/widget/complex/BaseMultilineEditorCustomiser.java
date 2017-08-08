package cc.alcina.framework.gwt.client.widget.complex;

import java.util.List;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;
import cc.alcina.framework.gwt.client.widget.Link;

public abstract class BaseMultilineEditorCustomiser<T extends HasIdAndLocalId>
		implements Customiser, BoundWidgetProvider {
	private boolean editable;

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom params) {
		this.editable = editable;
		return this;
	}

	public abstract void doCreateRow(Object model,
			BaseMultilineEditor<T> editor);

	public void doDeleteRows(List<T> items, BaseMultilineEditor<T> editor) {
		for (T t : items) {
			deleteItem(t);
			editor.getValue().remove(t);
		}
		editor.redraw();
	}

	protected abstract void deleteItem(T t);

	public abstract Class<T> getItemClass();

	@Override
	public BoundWidget get() {
		BaseMultilineEditor editor = asMultipleGrids()
				? new BaseMultilineGridEditor() : new BaseMultilineRowEditor();
		editor.setCustomiser(this);
		editor.setEditable(editable);
		return (BoundWidget) editor;
	}

	protected boolean asMultipleGrids() {
		return false;
	}

	public void customiseActions(List<PermissibleAction> actions) {
	}

	public boolean handleCustomAction(BaseMultilineEditor<T> editor,
			PermissibleAction action) {
		return false;
	}

	public abstract String getCreateActionDisplayName();

	public List<T> filterVisibleValues(List<T> values) {
		return values;
	}

	public List<Link> customisePerRowEditActions(List<Link> actions, T rowValue,
			BaseMultilineEditor editor) {
		return actions;
	}
}
