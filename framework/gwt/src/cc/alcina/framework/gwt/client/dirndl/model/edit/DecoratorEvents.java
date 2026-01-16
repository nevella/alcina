package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class DecoratorEvents {
	public static class DecoratorBound
			extends ModelEvent<Object, DecoratorBound.Handler> {
		@Override
		public void dispatch(DecoratorBound.Handler handler) {
			handler.onDecoratorBound(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onDecoratorBound(DecoratorBound event);
		}

		public interface Binding extends Handler {
			@Override
			default void onDecoratorBound(DecoratorBound event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}
}
