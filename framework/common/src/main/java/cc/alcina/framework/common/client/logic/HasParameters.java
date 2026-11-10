package cc.alcina.framework.common.client.logic;

public interface HasParameters<T> {
	T getParameters();

	void setParameters(T parameters);
}
