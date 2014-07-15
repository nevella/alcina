package cc.alcina.framework.common.client.util;

public interface TimerWrapper {
	public void cancel();

	public void scheduleRepeating(long periodMillis);
	
	public void scheduleSingle(long delayMillis);

	public interface TimerWrapperProvider {
		public TimerWrapper getTimer(Runnable runnable);
		
		public void scheduleDeferred(Runnable runnable);
		
		public void scheduleDeferredIfOnUIThread(Runnable runnable);
	}
}
