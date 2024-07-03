package cc.alcina.framework.gwt.client.dirndl.impl.form;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.validator.AbstractValidationFeedback;
import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.gwt.client.util.GwtDomUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class FmsValidationFeedback extends AbstractValidationFeedback {
	private FlowPanel validationWidget = null;

	@Override
	public void handleException(Object source, ValidationException exception) {
		final Widget w = (Widget) source;
		resolve(source);
		if (!GwtDomUtils.isVisibleAncestorChain(w.getElement())) {
			return;
		}
		FlowPanel ancestor = WidgetUtils.getAncestorWidget(w, FlowPanel.class);
		validationWidget = new FlowPanel();
		validationWidget.setStyleName("ol-form-message  -ol-invalid");
		Label label = new Label("p");
		label.setText(getMessage(exception));
		validationWidget.add(label);
		ancestor.add(validationWidget);
	}

	@Override
	public void resolve(Object source) {
		if (validationWidget != null) {
			validationWidget.removeFromParent();
		}
	}
}