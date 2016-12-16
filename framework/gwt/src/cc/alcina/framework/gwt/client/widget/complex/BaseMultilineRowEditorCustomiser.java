package cc.alcina.framework.gwt.client.widget.complex;

import java.util.List;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;

public abstract class BaseMultilineRowEditorCustomiser<T extends HasIdAndLocalId>
		implements Customiser, BoundWidgetProvider {
	private boolean editable;

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom params) {
		this.editable = editable;
		return this;
	}

	public abstract void doCreateRow(BoundTableExt table,
			BaseMultilineRowEditor<T> editor);

	public void doDeleteRows(BoundTableExt table,
			BaseMultilineRowEditor<T> editor) {
		List<T> selected = editor.provideSelected();
		for (T t : selected) {
			deleteItem(t);
			editor.getValue().remove(t);
		}
		editor.redraw();
	}

	protected abstract void deleteItem(T t);

	public abstract Class<T> getItemClass();

	@Override
	public BoundWidget get() {
		BaseMultilineRowEditor editor = new BaseMultilineRowEditor();
		editor.customiser = this;
		editor.setEditable(editable);
		return editor;
	}

	public void customiseActions(List<PermissibleAction> actions) {
	}

	public boolean handleCustomAction(MultilineRowEditor editor, PermissibleAction action) {
		return false;
	}
}
