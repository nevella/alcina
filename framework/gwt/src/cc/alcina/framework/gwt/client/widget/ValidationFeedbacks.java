package cc.alcina.framework.gwt.client.widget;

import cc.alcina.framework.common.client.gwittir.validator.NotNullValidator;
import cc.alcina.framework.common.client.gwittir.validator.StringHasLengthValidator;
import cc.alcina.framework.gwt.client.widget.CombiningValidationFeedback.CombiningValidationFeedbackCollector;

import com.totsp.gwittir.client.validator.AbstractValidationFeedback;
import com.totsp.gwittir.client.validator.CompositeValidationFeedback;
import com.totsp.gwittir.client.validator.StyleValidationFeedback;
import com.totsp.gwittir.client.validator.ValidationFeedback;

public class ValidationFeedbacks {
	private StyleValidationFeedback svf() {
		return new StyleValidationFeedback("err");
	}

	public CombiningValidationFeedbackCollector collector = new CombiningValidationFeedbackCollector();

	public AbstractValidationFeedback wrapCollector(
			ValidationFeedback feedback, String message) {
		return new CompositeValidationFeedback(feedback,
				new CombiningValidationFeedback(collector, message));
	}

	public AbstractValidationFeedback wrapStyleInCollector(String message) {
		return wrapCollector(svf(), message);
	}

	public AbstractValidationFeedback wrapStyleFeedback(
			ValidationFeedback otherFeedback) {
		return new CompositeValidationFeedback(otherFeedback, svf());
	}

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
}
