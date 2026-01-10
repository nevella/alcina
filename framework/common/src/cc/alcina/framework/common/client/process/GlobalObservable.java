package cc.alcina.framework.common.client.process;

public interface GlobalObservable extends ProcessObservable {
	/**
	 * An observable intended purely for debugging
	 */
	public interface Debug extends GlobalObservable {
	}
}