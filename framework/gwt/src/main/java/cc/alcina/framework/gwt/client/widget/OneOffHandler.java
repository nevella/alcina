package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.event.shared.HandlerRegistration;

public class OneOffHandler implements Runnable {
	private Runnable runnable;

	private HandlerRegistration registration;

	public OneOffHandler(Runnable runnable) {
		this.runnable = runnable;
	}

	public void register(HandlerRegistration registration) {
		this.registration = registration;
	}

	@Override
	public void run() {
		registration.removeHandler();
		runnable.run();
	}
}