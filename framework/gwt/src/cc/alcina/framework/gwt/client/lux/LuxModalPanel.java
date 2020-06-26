package cc.alcina.framework.gwt.client.lux;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.ide.ContentViewSections;
import cc.alcina.framework.gwt.client.lux.LuxStyle.LuxStyleModal;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public abstract class LuxModalPanel extends Composite {
	FlowPanel fp = new FlowPanel();

	protected List<ContentViewSections> builders = new ArrayList<>();

	protected Widget statusPanel;

	protected LuxButton defaultButton;

	public LuxModalPanel() {
		initWidget(fp);
		LuxStyleModal.LUX_MODAL_PANEL.addTo(this);
		LuxStyle.LUX.addTo(this);
	}

	protected ContentViewSections createBuilder() {
		ContentViewSections builder = new ContentViewSections();
		builder.editable();
		builder.setAutoSave(true);
		builders.add(builder);
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

	protected void createStatusPanel() {
		statusPanel = new LuxStatusPanel();
	}

	protected void onFormSubmit() {
		if (defaultButton != null) {
			DomEvent.fireNativeEvent(WidgetUtils.createZeroClick(),
					defaultButton);
		}
	}

	protected void render() {
		builders.clear();
		fp.add(LuxStyleModal.LUX_MODAL_PANEL.addTo(createHeaderPanel()));
		FormPanel formPanel = new FormPanel();
		formPanel.add(createContentPanel());
		formPanel.setAction("submit.do");
		formPanel.addSubmitHandler(e -> {
			e.cancel();
			WidgetUtils.squelchCurrentEvent();
			onFormSubmit();
		});
		fp.add(formPanel);
		createStatusPanel();
		fp.add(statusPanel);
		fp.add(createButtonsPanel());
		Widget footer = createFooterPanel();
		if (footer != null) {
			fp.add(footer);
		}
	}

	protected boolean validate() {
		for (ContentViewSections sectionsBuilder : builders) {
			// FIXME - directedlayout.2 - scroll into view - validate should
			// return a
			// validationresult w widget
			//
			// check Jumail's rework of validation?
			if (!sectionsBuilder.validateSync()) {
				return false;
			}
		}
		return true;
	}
}
