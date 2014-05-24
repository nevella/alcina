package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.Collection;

public interface DomainModelObject extends Serializable{
	public Collection registrableObjects();
	
	public void ensureLookups();
}
