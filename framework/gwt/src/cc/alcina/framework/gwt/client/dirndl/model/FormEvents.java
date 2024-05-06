package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Objects;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.gwittir.validator.ValidationState;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.TableRow;

public class FormEvents {
	/*
	 * Note that a result can be intermediate - i.e. with a non-complete state
	 */
	public static class ValidationResult extends Bindable.Fields {
		public ValidationState state;

		public String exceptionMessage;

		transient ModelEvent originatingEvent;

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

		ValidationResult(ModelEvent originatingEvent, ValidationState state,
				String exceptionMessage) {
			this.originatingEvent = originatingEvent;
			this.state = state;
			this.exceptionMessage = exceptionMessage;
		}

		public ValidationResult() {
		}

		public ValidationResult(ValidationState state) {
			this(null, state, null);
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

		public static ValidationResult invalid(Throwable e) {
			return invalid(CommonUtils.toSimpleExceptionMessage(e));
		}

		public static ValidationResult invalid(String message) {
			return new ValidationResult(null, ValidationState.INVALID, message);
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

	public static class ValidationResultEvent extends
			ModelEvent<FormEvents.ValidationResult, ValidationResultEvent.Handler> {
		@Override
		public void dispatch(ValidationResultEvent.Handler handler) {
			handler.onValidationResult(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onValidationResult(ValidationResultEvent event);
		}
	}

	public static class RowClicked
			extends ModelEvent<TableRow, RowClicked.Handler> {
		@Override
		public void dispatch(RowClicked.Handler handler) {
			handler.onRowClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onRowClicked(RowClicked event);
		}
	}
}
