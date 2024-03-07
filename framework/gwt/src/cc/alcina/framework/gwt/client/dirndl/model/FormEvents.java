package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class FormEvents {
	/*
	 * Emitted during form validation by FormValidation, and handled by
	 * FormModel
	 */
	public static class BeanValidationChange extends
			ModelEvent<FormEvents.ValidationResult, BeanValidationChange.Handler> {
		@Override
		public void dispatch(BeanValidationChange.Handler handler) {
			handler.onBeanValidationChange(this);
		}

		public boolean isComplete() {
			return getModel().state.isComplete();
		}

		public boolean isValid() {
			return getModel().state.isValid();
		}

		@Override
		public String toString() {
			return getModel().toString();
		}

		public interface Handler extends NodeEvent.Handler {
			void onBeanValidationChange(BeanValidationChange event);
		}
	}

	public static class ValidationResult {
		public FormEvents.ValidationState state;

		public String exceptionMessage;

		ModelEvent originatingEvent;

		ValidationResult(ModelEvent originatingEvent,
				FormEvents.ValidationState state, String exceptionMessage) {
			this.originatingEvent = originatingEvent;
			this.state = state;
			this.exceptionMessage = exceptionMessage;
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("state", state, "exceptionMessage",
					exceptionMessage);
		}
	}

	public static class PropertyValidationChange
			extends ModelEvent<Boolean, PropertyValidationChange.Handler> {
		@Override
		public void dispatch(PropertyValidationChange.Handler handler) {
			handler.onPropertyValidationChange(this);
		}

		public boolean isValid() {
			return getModel();
		}

		public interface Handler extends NodeEvent.Handler {
			void onPropertyValidationChange(PropertyValidationChange event);
		}
	}

	/*
	 * A model whose descendant is transformed into a FormModel can emit this
	 * event - the FormModel will respond with a QueryValidityResult event
	 */
	public static class QueryValidity extends
			ModelEvent.DescendantEvent<Object, QueryValidity.Handler, QueryValidity.Emitter> {
		@Override
		public void dispatch(QueryValidity.Handler handler) {
			handler.onQueryValidity(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onQueryValidity(QueryValidity event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}

	public static class QueryValidityResult extends
			ModelEvent<FormEvents.ValidationResult, QueryValidityResult.Handler> {
		@Override
		public void dispatch(QueryValidityResult.Handler handler) {
			handler.onQueryValidityResult(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onQueryValidityResult(QueryValidityResult event);
		}
	}

	public enum ValidationState {
		VALIDATING, ASYNC_VALIDATING, VALID, INVALID;

		boolean isComplete() {
			switch (this) {
			case VALIDATING:
			case ASYNC_VALIDATING:
				return false;
			default:
				return true;
			}
		}

		boolean isValid() {
			return this == VALID;
		}
	}
}
