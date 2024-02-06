package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.FormValidation.State;

public class FormEvents {
	public static class BeanValidationChange extends
			ModelEvent<BeanValidationChange.Data, BeanValidationChange.Handler> {
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

		static class Data {
			FormValidation.State state;

			String exceptionMessage;

			ModelEvent originatingEvent;

			Data(ModelEvent originatingEvent, State state,
					String exceptionMessage) {
				this.originatingEvent = originatingEvent;
				this.state = state;
				this.exceptionMessage = exceptionMessage;
			}

			@Override
			public String toString() {
				return FormatBuilder.keyValues("state", state,
						"exceptionMessage", exceptionMessage);
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onBeanValidationChange(BeanValidationChange event);
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
}
