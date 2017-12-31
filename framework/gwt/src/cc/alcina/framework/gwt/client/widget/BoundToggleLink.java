package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

public class BoundToggleLink extends AbstractBoundWidget<Boolean>
		implements SelectionHandler<Integer> {
	protected ToggleLink base;

	public BoundToggleLink() {
	}

	public BoundToggleLink(String label1, String label2) {
		base = new ToggleLink(label1, label2, this);
		initWidget(base);
	}

	public void addButtonStyleName(String style) {
		base.addButtonStyleName(style);
	}

	@Override
	public Boolean getValue() {
		return base.getSelectedIndex() != 0;
	}

	@Override
	public void onSelection(SelectionEvent<Integer> event) {
		setValue(event.getSelectedItem() != 0);
		this.changes.firePropertyChange("value",
				!this.getValue().booleanValue(),
				this.getValue().booleanValue());
	}

	@Override
	public void setValue(Boolean value) {
		Boolean old = this.getValue();
		base.setSelectedIndex(value ? 1 : 0);
		if ((old != this.getValue()) && !old.equals(this.getValue())) {
			this.changes.firePropertyChange("value", old, this.getValue());
		}
	}
}
