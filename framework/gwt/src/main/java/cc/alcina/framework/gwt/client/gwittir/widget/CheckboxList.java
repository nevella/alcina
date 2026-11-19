package cc.alcina.framework.gwt.client.gwittir.widget;

import java.util.Collection;

import com.google.gwt.user.client.ui.CheckBox;
import java.util.function.Function;

public class CheckboxList<T> extends RadioButtonList<T> {
	public CheckboxList() {
	}

	public CheckboxList(String groupName, Collection<T> values,
			Function renderer, int columnCount) {
		super(groupName, values, renderer, columnCount);
	}

	@Override
	protected CheckBox createCheckBox(String displayText) {
		return new CheckBox(displayText, true);
	}

	@Override
	protected boolean singleResult() {
		return false;
	}
}