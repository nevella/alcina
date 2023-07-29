package cc.alcina.framework.gwt.client.logic;

import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.*;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.reflection.Reflections;

/**
 * <p>
 * For lightweight usage of the base history classes, transformmanager
 * references moved here
 * </p>
 * 
 * 
 * 
 */
public class AlcinaHistoryItemTm extends AlcinaHistoryItem {
	public Object getReferencedObjectOrClassName() {
		if (getClassName() == null) {
			return null;
		}
		if (getId() == 0 && getLocalId() == 0) {
			return getClassName();
		}
		return TransformManager.get().getObjectStore().getObject(
				Reflections.forName(getClassName()), getId(), getLocalId());
	}

	public void setReferencedObject(Entity entity) {
		setParameter(CLASS_NAME_KEY, entity.getClass().getName());
		setParameter(ID_KEY, entity.getId());
		setParameter(LOCAL_ID_KEY, entity.getLocalId());
	}
}
