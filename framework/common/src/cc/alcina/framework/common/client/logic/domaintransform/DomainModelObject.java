package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.Collection;

import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;

@ReflectiveSerializer.Checks(hasReflectedSubtypes = true)
public interface DomainModelObject extends Serializable {
	public void ensureLookups();

	public Collection registrableObjects();
}
