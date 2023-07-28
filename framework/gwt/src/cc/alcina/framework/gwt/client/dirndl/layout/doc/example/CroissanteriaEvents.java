package cc.alcina.framework.gwt.client.dirndl.layout.doc.example;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

/**
 * Events handled by {@link Croissanteria} need to be in a separate class, since
 * it implements their {@link Handler} interfaces
 *
 */
public class CroissanteriaEvents {
	public static class TrySample extends ModelEvent<Object, TrySample.Handler> {
		@Override
		public void dispatch(TrySample.Handler handler) {
			handler.onTrySample(this);
		}
		
		@Override
		public Class<TrySample.Handler> getHandlerClass() {
			return TrySample.Handler.class;
		}
		
		public interface Handler extends NodeEvent.Handler {
			void onTrySample(TrySample event);
		}
	}
}
