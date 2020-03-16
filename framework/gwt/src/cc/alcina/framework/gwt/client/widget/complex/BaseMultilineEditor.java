package cc.alcina.framework.gwt.client.widget.complex;

import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.Entity.EntityComparatorLocalsHigh;

public interface BaseMultilineEditor<T extends Entity> {
	public Object getModel();

	public Set<T> getValue();

	public List<T> provideSelected();

	public void redraw();

	public void setEditable(boolean editable);
	
	void setCustomiser(BaseMultilineEditorCustomiser<T> customiser);

	default void sortValues(List<T> values) {
		values.sort(Entity.EntityComparatorLocalsHigh.INSTANCE);
	}
}
