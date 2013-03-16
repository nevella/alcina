package cc.alcina.framework.common.client.search;

public interface DeepCloneable<T> {
	public T deepClone() throws CloneNotSupportedException;
}
