package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;

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

	public static class DecoratorsChanged
			extends ModelEvent<List<DecoratorNode>, DecoratorsChanged.Handler> {
		@Override
		public void dispatch(DecoratorsChanged.Handler handler) {
			handler.onDecoratorsChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onDecoratorsChanged(DecoratorsChanged event);
		}

		public interface Binding extends Handler {
			@Override
			default void onDecoratorsChanged(DecoratorsChanged event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}
}
