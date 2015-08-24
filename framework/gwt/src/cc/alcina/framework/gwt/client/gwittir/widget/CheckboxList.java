package cc.alcina.framework.gwt.client.gwittir.widget;

import java.util.Collection;

import com.google.gwt.user.client.ui.CheckBox;
import com.totsp.gwittir.client.ui.Renderer;

public class CheckboxList<T> extends RadioButtonList<T> {
	public CheckboxList() {
	}

	public CheckboxList(String groupName, Collection<T> values,
			Renderer renderer, int columnCount) {
		super(groupName, values, renderer, columnCount);
	}

	@Override
	protected boolean singleResult() {
		return false;
	}

	@Override
	protected CheckBox createCheckBox(String displayText) {
		return new CheckBox(displayText, true);
	}
}