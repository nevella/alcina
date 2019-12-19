package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.ide.ContentViewSections;

public abstract class LuxModalPanel extends Composite {
	FlowPanel fp = new FlowPanel();

	public LuxModalPanel() {
		initWidget(fp);
	}

	protected ContentViewSections createBuilder() {
		ContentViewSections builder = new ContentViewSections();
		builder.editable();
		builder.setAutoSave(true);
		return builder;
	}

	protected Widget createButtonsPanel() {
		return new Label("Buttons");
	}

	protected abstract Widget createContentPanel();

	protected Widget createFooterPanel() {
		return null;
	}

	protected Widget createHeaderPanel() {
		return new Label("Default header");
	}

	protected void render() {
		fp.add(createHeaderPanel());
		fp.add(createContentPanel());
		fp.add(createButtonsPanel());
		Widget footer = createFooterPanel();
		if (footer != null) {
			fp.add(footer);
		}
	}
}
