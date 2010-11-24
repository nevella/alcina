package cc.alcina.framework.common.client.util;

public interface TimerWrapper {
	public void cancel();

	public void scheduleRepeating(long periodMillis);

	public interface TimerWrapperProvider {
		public TimerWrapper getTimer(Runnable runnable);
	}
}
