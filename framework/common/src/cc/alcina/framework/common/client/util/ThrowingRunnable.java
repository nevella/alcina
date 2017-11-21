package cc.alcina.framework.common.client.util;

@FunctionalInterface
public interface ThrowingRunnable {
	public void run() throws Exception;
}