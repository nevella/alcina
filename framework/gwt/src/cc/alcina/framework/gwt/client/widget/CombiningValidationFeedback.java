package cc.alcina.framework.gwt.client.widget;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.gwt.client.ClientLayerLocator;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.validator.AbstractValidationFeedback;
import com.totsp.gwittir.client.validator.ValidationException;

public class CombiningValidationFeedback extends AbstractValidationFeedback {
	protected final CombiningValidationFeedbackCollector collector;

	private final String message;

	public CombiningValidationFeedback(
			CombiningValidationFeedbackCollector collector) {
		this(collector, null);
	}

	public CombiningValidationFeedback(
			CombiningValidationFeedbackCollector collector, String message) {
		this.collector = collector;
		this.message = message;
	}

	@Override
	public void handleException(Object source, ValidationException exception) {
		ValidationException messagingException = exception;
		// pass-through subtypes
		if (messagingException.getClass() == ValidationException.class) {
			messagingException = new ValidationException(
					message == null ? getMessage(exception) : message);
		}
		collector.addException(source, messagingException);
	}

	public boolean hasExceptions() {
		return !collector.exceptions.isEmpty();
	}

	@Override
	public void resolve(Object source) {
		collector.resolve(source);
	}

	public static class CombiningValidationFeedbackCollector {
		protected Map<Object, ValidationException> exceptions = new LinkedHashMap<Object, ValidationException>();

		private String caption = "Please correct the following";

		public void clear() {
			exceptions.clear();
		}

		public void resolve(Object source) {
			exceptions.remove(source);
		}

		public void show() {
			FlowPanel fp = new FlowPanel();
			fp.setStyleName("combined-validation-feedback");
			Label caption = new Label(getCaption());
			caption.setStyleName("caption");
			fp.add(caption);
			ULPanel ulPanel = new ULPanel();
			fp.add(ulPanel);
			for (ValidationException e : exceptions.values()) {
				Widget child = null;
				if (e instanceof ValidationExceptionWithHtmlMessage) {
					ValidationExceptionWithHtmlMessage withHtml = (ValidationExceptionWithHtmlMessage) e;
					child = new InlineHTML(withHtml.getSafeHtml());
				} else {
					child = new InlineLabel(e.getMessage());
				}
				ulPanel.add(new LiPanel(child));
			}
			showFeedbackPanel(fp);
		}

		public String getCaption() {
			return caption;
		}

		protected void showFeedbackPanel(FlowPanel fp) {
			ClientLayerLocator.get().notifications().showMessage(fp);
		}

		void addException(Object source, ValidationException exception) {
			exceptions.put(source, exception);
		}
		public Collection<ValidationException> getExceptions(){
			return exceptions.values();
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}
	}

	public static class ValidationExceptionWithHtmlMessage extends
			ValidationException {
		private ValidationException source;

		public ValidationExceptionWithHtmlMessage(ValidationException source) {
			super(source.getMessage());
			this.source = source;
		}

		public ValidationExceptionWithHtmlMessage(String htmlMessage,
				Class validatorClass) {
			super(htmlMessage, validatorClass);
			safeHtml = SafeHtmlUtils.fromTrustedString(htmlMessage);
			this.source = this;
		}

		private SafeHtml safeHtml;

		public SafeHtml getSafeHtml() {
			return this.safeHtml;
		}

		public void setSafeHtml(SafeHtml safeHtml) {
			this.safeHtml = safeHtml;
		}

		public ValidationException getSource() {
			return this.source;
		}
	}
}
