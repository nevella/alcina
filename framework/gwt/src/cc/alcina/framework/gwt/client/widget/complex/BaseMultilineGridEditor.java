package cc.alcina.framework.gwt.client.widget.complex;

import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.gwt.client.widget.Link;

public class BaseMultilineGridEditor<T extends HasIdAndLocalId>
		extends MultilineGridEditor<T> implements BaseMultilineEditor<T> {
	BaseMultilineEditorCustomiser<T> customiser;

	@Override
	public String getCreateActionDisplayName() {
		return customiser.getCreateActionDisplayName();
	}

	public List<T> provideSelected() {
		return table.getSelected();
	}

	@Override
	public void setCustomiser(BaseMultilineEditorCustomiser<T> customiser) {
		this.customiser = customiser;
	}

	@Override
	protected List<Link> createPerRowEditActions(T rowValue) {
		List<Link> actions = super.createPerRowEditActions(rowValue);
		actions = customiser.customisePerRowEditActions(actions, rowValue,
				this);
		return actions;
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

	@Override
	protected List<T> filterVisibleValues(List<T> values) {
		return customiser.filterVisibleValues(values);
	}

	@Override
	protected boolean handleCustomAction(MultilineGridEditor editor,
			PermissibleAction action) {
		return customiser.handleCustomAction(this, action);
	}

	BaseMultilineEditorCustomiser<T> getCustomiser() {
		return null;
	}
}