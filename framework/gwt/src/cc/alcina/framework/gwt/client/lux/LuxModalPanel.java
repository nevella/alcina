package cc.alcina.framework.gwt.client.lux;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.ide.ContentViewSections;
import cc.alcina.framework.gwt.client.lux.LuxStyle.LuxStyleModal;

public abstract class LuxModalPanel extends Composite {
	FlowPanel fp = new FlowPanel();

	// FIXME - move to 'validation support'
	protected List<ContentViewSections> builders = new ArrayList<>();

	public LuxModalPanel() {
		initWidget(fp);
		LuxStyleModal.LUX_MODAL_PANEL.add(this);
		LuxStyle.LUX.add(this);
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
		fp.add(LuxStyleModal.LUX_MODAL_PANEL.add(createHeaderPanel()));
		fp.add(createContentPanel());
		fp.add(createButtonsPanel());
		Widget footer = createFooterPanel();
		if (footer != null) {
			fp.add(footer);
		}
	}

	protected boolean validate() {
		for (ContentViewSections sectionsBuilder : builders) {
			// FIXME - scroll into view - validate should return a
			// validationresult w widget
			if (!sectionsBuilder.validateSync()) {
				return false;
			}
		}
		return true;
	}
}
