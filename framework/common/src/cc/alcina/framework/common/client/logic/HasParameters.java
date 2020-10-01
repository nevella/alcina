package cc.alcina.framework.common.client.logic;

public interface HasParameters<T> {

	void setParameters(T parameters);

	T getParameters();
}
