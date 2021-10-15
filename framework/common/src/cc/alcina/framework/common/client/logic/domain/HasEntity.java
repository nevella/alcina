package cc.alcina.framework.common.client.logic.domain;

import java.io.Serializable;

public interface HasEntity<E extends Entity> extends Serializable {
	public E provideEntity();
}
