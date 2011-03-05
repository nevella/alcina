package cc.alcina.framework.common.client.logic.domain;

public interface HasValue<T> {
	public T getValue();
	public void setValue(T t);
}
