package cc.alcina.framework.gwt.client.widget;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.validator.AbstractValidationFeedback;
import com.totsp.gwittir.client.validator.CompositeValidationFeedback;
import com.totsp.gwittir.client.validator.StyleValidationFeedback;
import com.totsp.gwittir.client.validator.ValidationFeedback;

import cc.alcina.framework.common.client.gwittir.validator.NotNullValidator;
import cc.alcina.framework.common.client.gwittir.validator.StringHasLengthValidator;
import cc.alcina.framework.gwt.client.widget.CombiningValidationFeedback.CombiningValidationFeedbackCollector;

public class ValidationFeedbacks {
	public CombiningValidationFeedbackCollector collector = new CombiningValidationFeedbackCollector();

	public AbstractValidationFeedback createValidationFeedback() {
		return createValidationFeedback(RelativePopupValidationFeedback.BOTTOM,
				null);
	}

	public AbstractValidationFeedback createValidationFeedback(int pos,
			String requiredMessage) {
		requiredMessage = requiredMessage == null ? "Field is required"
				: requiredMessage;
		RelativePopupValidationFeedback feedback = new RelativePopupValidationFeedback(
				pos);
		feedback.addMessage(StringHasLengthValidator.class, requiredMessage);
		feedback.addMessage(NotNullValidator.class, requiredMessage);
		return wrapCollector(wrapStyleFeedback(feedback), null);
	}

	public void wrapBindingsInCollector(Binding binding) {
		for (Binding b : binding.provideAllBindings(null)) {
			if (b.getLeft() != null && b.getLeft().feedback != null) {
				b.getLeft().feedback = wrapCollector(b.getLeft().feedback,
						null);
			}
		}
	}

	public AbstractValidationFeedback wrapCollector(ValidationFeedback feedback,
			String message) {
		return new CompositeValidationFeedback(feedback,
				new CombiningValidationFeedback(collector, message));
	}

	public AbstractValidationFeedback
			wrapStyleFeedback(ValidationFeedback otherFeedback) {
		return new CompositeValidationFeedback(otherFeedback, svf());
	}

	public AbstractValidationFeedback wrapStyleInCollector(String message) {
		return wrapCollector(svf(), message);
	}

	private StyleValidationFeedback svf() {
		return new StyleValidationFeedback("err");
	}
}
