package cc.alcina.framework.gwt.client.logic;

import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.*;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

/**
 * <p>
 * For lightweight usage of the base history classes, transformmanager
 * references moved here
 * </p>
 * 
 * @author nick@alcina.cc
 * 
 */
public class AlcinaHistoryItemTm extends AlcinaHistoryItem {
	@SuppressWarnings("unchecked")
	public Object getReferencedObjectOrClassName() {
		if (getClassName() == null) {
			return null;
		}
		if (getId() == 0 && getLocalId() == 0) {
			return getClassName();
		}
		return TransformManager.get().getObject(
				Reflections.classLookup().getClassForName(getClassName()),
				getId(), getLocalId());
	}

	public void setReferencedObject(Entity entity) {
		setParameter(CLASS_NAME_KEY, entity.getClass().getName());
		setParameter(ID_KEY, entity.getId());
		setParameter(LOCAL_ID_KEY, entity.getLocalId());
	}
}
