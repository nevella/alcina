package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

import cc.alcina.framework.gwt.client.widget.complex.ContextMenuHelper;

public abstract class DropdownBase extends Composite {
	protected FlowPanel dropdownPanel;

	protected ContextMenuHelper contextMenuHelper;

	public DropdownBase() {
		this.dropdownPanel = new FlowPanel();
		dropdownPanel.setStyleName("search-popdown-menu");
		this.contextMenuHelper = new ContextMenuHelper();
		initWidget(dropdownPanel);
		render();
	}

	protected abstract void render();
}