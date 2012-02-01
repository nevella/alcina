package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.util.TimerWrapper;
import cc.alcina.framework.gwt.client.ClientLayerLocator;

public class AtEndOfEventSeriesTimer {
	private long lastEventOccurred = 0;

	private Runnable checkCallback=new Runnable() {
		@Override
		public void run() {
			if(System.currentTimeMillis()-lastEventOccurred>=waitToPerformAction){
				timer.cancel();
				timer=null;
				action.run();
			}
		}
	};

	private final long waitToPerformAction;

	private final Runnable action;

	public AtEndOfEventSeriesTimer(long waitToPerformAction, Runnable action) {
		this.waitToPerformAction = waitToPerformAction;
		this.action = action;
	}

	private TimerWrapper timer = null;

	public void triggerEventOccurred() {
		lastEventOccurred = System.currentTimeMillis();
		if (timer == null) {
			timer = ClientLayerLocator.get().timerWrapperProvider()
					.getTimer(checkCallback);
			timer.scheduleRepeating(waitToPerformAction / 2);
		}
	}

	public void cancel() {
		if(timer!=null){
			timer.cancel();
			timer=null;
		}
		
	}
}
