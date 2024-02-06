package cc.alcina.framework.gwt.client.widget.complex;

import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface BaseMultilineEditor<T extends Entity> {
	public Object getModel();

	public Set<T> getValue();

	public List<T> provideSelected();

	public void redraw();

	void setCustomiser(BaseMultilineEditorCustomiser<T> customiser);

	public void setEditable(boolean editable);

	default void sortValues(List<T> values) {
		values.sort(Entity.EntityComparatorLocalsHigh.INSTANCE);
	}
}
