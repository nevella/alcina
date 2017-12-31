package cc.alcina.framework.gwt.client.widget.complex;

import java.util.List;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;

public class BaseMultilineRowEditor<T extends HasIdAndLocalId>
		extends MultilineRowEditor<T> implements BaseMultilineEditor<T> {
	private BaseMultilineEditorCustomiser<T> customiser;

	public BaseMultilineEditorCustomiser<T> getCustomiser() {
		return this.customiser;
	}

	public List<T> provideSelected() {
		return table.getSelected();
	}

	public void setCustomiser(BaseMultilineEditorCustomiser<T> customiser) {
		this.customiser = customiser;
	}

	@Override
	public void sortValues(List<T> values) {
		customiser.sortValues(values);
	}

	@Override
	protected void customiseActions(List<PermissibleAction> actions) {
		customiser.customiseActions(actions);
	}

	@Override
	protected void customiseContentViewFactory(
			ContentViewFactory contentViewFactory, Object model) {
		customiser.customiseContentViewFactory(contentViewFactory, model);
	}

	@Override
	protected void doCreateRow() {
		customiser.doCreateRow(getModel(), this);
	}

	@Override
	protected void doDeleteRows() {
		customiser.doDeleteRows(table.getSelected(), this);
	}

	@Override
	protected List<T> filterVisibleValues(List<T> values) {
		return customiser.filterVisibleValues(values);
	}

	@Override
	protected Class<T> getItemClass() {
		return customiser.getItemClass();
	}

	@Override
	protected boolean handleCustomAction(MultilineRowEditor editor,
			PermissibleAction action) {
		return customiser.handleCustomAction(this, action);
	}
}