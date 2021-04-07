package cc.alcina.framework.common.client.logic.domain;

public interface HasEntity<E extends Entity> {
	public E provideEntity();
}
