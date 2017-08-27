package cc.alcina.framework.gwt.client.widget;

import java.util.List;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;

public class VariableWizardVisibilityHelper<U> {
	Multimap<U, List<Widget>> toShowWidgets = new Multimap<U, List<Widget>>();

	public Widget createRequiredFieldCaption(String captionText,
			U toShowSection) {
		HTML w = new HTML(Ax.format(
				"<span>%s</span><span class='req-risk'>*</span>", captionText));
		w.setStyleName("form-caption");
		toShowWidgets.add(toShowSection, w);
		return w;
	}

	public Widget createEmptyLabel(U toShowSection) {
		Widget w = UsefulWidgetFactory.createEmptyLabel();
		toShowWidgets.add(toShowSection, w);
		return w;
	}

	public Widget createFieldCaption(String captionText, U toShowSection) {
		HTML w = new HTML(Ax.format("<span>%s</span>", captionText));
		w.setStyleName("form-caption");
		toShowWidgets.add(toShowSection, w);
		return w;
	}

	public Widget createFieldCaption(String captionText) {
		return createFieldCaption(captionText, null);
	}

	public Widget createRequiredFieldCaption(String captionText) {
		return createRequiredFieldCaption(captionText, null);
	}

	public void showSection(U sectionId, boolean show) {
		for (Widget w : toShowWidgets.get(sectionId)) {
			w.setVisible(show);
		}
	}

	public Multimap<U, List<Widget>> getToShowWidgets() {
		return this.toShowWidgets;
	}
}