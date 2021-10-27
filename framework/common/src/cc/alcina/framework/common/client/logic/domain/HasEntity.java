package cc.alcina.framework.common.client.logic.domain;

import java.io.Serializable;

public interface HasEntity extends Serializable {
	public Entity provideEntity();
}
