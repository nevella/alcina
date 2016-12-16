package cc.alcina.framework.gwt.client.widget.complex;

import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface BaseMultilineEditor<T extends HasIdAndLocalId> {
	public void setEditable(boolean editable);

	void setCustomiser(BaseMultilineEditorCustomiser<T> customiser);

	public List<T> provideSelected();

	public Set<T> getValue();

	public void redraw();

	public Object getModel();
}
