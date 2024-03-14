package cc.alcina.framework.gwt.client.dirndl.layout.doc.example;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

/**
 * Events handled by {@link Croissanteria} need to be in a separate class, since
 * it implements their {@link Node.Handler} interfaces
 *
 */
public class CroissanteriaEvents {
	public static class BiteSample
			extends ModelEvent<Object, BiteSample.Handler> {
		@Override
		public void dispatch(BiteSample.Handler handler) {
			handler.onTrySample(this);
		}

		@Override
		public Class<BiteSample.Handler> getHandlerClass() {
			return BiteSample.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onTrySample(BiteSample event);
		}
	}
}
