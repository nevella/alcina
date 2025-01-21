package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Objects;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.gwittir.validator.ValidationState;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class FormEvents {
	/*
	 * Note that a result can be intermediate - i.e. with a non-complete state
	 */
	public static class ValidationResult extends Bindable.Fields {
		public static ValidationResult invalid(Throwable e) {
			return invalid(CommonUtils.toSimpleExceptionMessage(e));
		}

		public static ValidationResult invalid(String message) {
			return new ValidationResult(null, ValidationState.INVALID, message);
		}

		public ValidationState state;

		public String exceptionMessage;

		transient ModelEvent originatingEvent;

		public ValidationResult() {
		}

		public ValidationResult(ValidationState state) {
			this(null, state, null);
		}

		ValidationResult(ModelEvent originatingEvent, ValidationState state,
				String exceptionMessage) {
			this.originatingEvent = originatingEvent;
			this.state = state;
			this.exceptionMessage = exceptionMessage;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ValidationResult) {
				ValidationResult o = (ValidationResult) obj;
				return CommonUtils.equals(state, o.state, exceptionMessage,
						o.exceptionMessage, originatingEvent,
						o.originatingEvent);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(state, exceptionMessage, originatingEvent);
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("state", state, "exceptionMessage",
					exceptionMessage);
		}

		@Property.Not
		public boolean isValidationComplete() {
			return state.isComplete();
		}

		public ValidationResult copy() {
			return new ValidationResult(originatingEvent, state,
					exceptionMessage);
		}
	}

	public static class PropertyValidationChange
			extends ModelEvent<Boolean, PropertyValidationChange.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onPropertyValidationChange(PropertyValidationChange event);
		}

		@Override
		public void dispatch(PropertyValidationChange.Handler handler) {
			handler.onPropertyValidationChange(this);
		}

		public boolean isValid() {
			return getModel();
		}
	}

	/*
	 * A model whose descendant is transformed into a FormModel can emit this
	 * event - the FormModel will respond with a QueryValidityResult event
	 */
	public static class QueryValidity extends
			ModelEvent.DescendantEvent<Object, QueryValidity.Handler, QueryValidity.Emitter> {
		public interface Handler extends NodeEvent.Handler {
			void onQueryValidity(QueryValidity event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		@Override
		public void dispatch(QueryValidity.Handler handler) {
			handler.onQueryValidity(this);
		}
	}

	public static class ValidationResultEvent extends
			ModelEvent<FormEvents.ValidationResult, ValidationResultEvent.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onValidationResult(ValidationResultEvent event);
		}

		@Override
		public void dispatch(ValidationResultEvent.Handler handler) {
			handler.onValidationResult(this);
		}
	}

	public static class ValidationFailed
			extends ModelEvent<String, ValidationFailed.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onValidationFailed(ValidationFailed event);
		}

		@Override
		public void dispatch(ValidationFailed.Handler handler) {
			handler.onValidationFailed(this);
		}
	}

	public static class ValidationSuccessMultiple
			extends ModelEvent<Object, ValidationSuccessMultiple.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onValidationSuccessMultiple(ValidationSuccessMultiple event);
		}

		@Override
		public void dispatch(ValidationSuccessMultiple.Handler handler) {
			handler.onValidationSuccessMultiple(this);
		}
	}
}
