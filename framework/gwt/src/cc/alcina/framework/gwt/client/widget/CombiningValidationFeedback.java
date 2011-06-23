package cc.alcina.framework.gwt.client.widget;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.gwt.client.ClientLayerLocator;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.totsp.gwittir.client.validator.AbstractValidationFeedback;
import com.totsp.gwittir.client.validator.ValidationException;

public class CombiningValidationFeedback extends AbstractValidationFeedback {
	private final CombiningValidationFeedbackCollector collector;

	private final String message;

	@Override
	public void handleException(Object source, ValidationException exception) {
		collector.addException(source, message == null ? exception
				: new ValidationException(message));
	}

	public CombiningValidationFeedback(
			CombiningValidationFeedbackCollector collector) {
		this(collector, null);
	}

	public CombiningValidationFeedback(
			CombiningValidationFeedbackCollector collector, String message) {
		this.collector = collector;
		this.message = message;
	}

	public static class CombiningValidationFeedbackCollector {
		Map<Object, ValidationException> exceptions = new LinkedHashMap<Object, ValidationException>();

		void addException(Object source, ValidationException exception) {
			exceptions.put(source, exception);
		}

		public void show() {
			FlowPanel fp = new FlowPanel();
			fp.setStyleName("combined-validation-feedback");
			Label caption = new Label("Please correct the following");
			caption.setStyleName("caption");
			fp.add(caption);
			ULPanel ulPanel = new ULPanel();
			fp.add(ulPanel);
			for (ValidationException e : exceptions.values()) {
				ulPanel.add(new LiPanel(new InlineLabel(e.getMessage())));
			}
			ClientLayerLocator.get().notifications().showMessage(fp);
		}

		public void clear() {
			exceptions.clear();
		}

		public void resolve(Object source) {
			exceptions.remove(source);
		}
	}

	@Override
	public void resolve(Object source) {
		collector.resolve(source);
	}
}
