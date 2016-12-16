package cc.alcina.framework.gwt.client.widget.complex;

import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class BaseMultilineGridEditor<T extends HasIdAndLocalId>
		extends MultilineGridEditor<T> implements BaseMultilineEditor<T> {
	BaseMultilineEditorCustomiser<T> customiser;

	@Override
	protected boolean handleCustomAction(MultilineGridEditor editor,
			PermissibleAction action) {
		return customiser.handleCustomAction(this, action);
	}

	@Override
	protected void customiseActions(List<PermissibleAction> actions) {
		customiser.customiseActions(actions);
	}

	@Override
	protected void doCreateRow() {
		customiser.doCreateRow(getModel(), this);
	}

	protected void doDeleteRow(T t) {
		customiser.doDeleteRows(Collections.singletonList(t), this);
	}

	public List<T> provideSelected() {
		return table.getSelected();
	}

	BaseMultilineEditorCustomiser<T> getCustomiser() {
		return null;
	}

	@Override
	public void setCustomiser(BaseMultilineEditorCustomiser<T> customiser) {
		this.customiser = customiser;
	}

	@Override
	public String getCreateActionDisplayName() {
		return customiser.getCreateActionDisplayName();
	}
}