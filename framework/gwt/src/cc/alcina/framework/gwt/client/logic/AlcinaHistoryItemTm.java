package cc.alcina.framework.gwt.client.logic;

import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.*;
import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
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
		return TransformManager.get()
				.getObject(
						Reflections.classLookup()
								.getClassForName(getClassName()), getId(),
						getLocalId());
	}

	public void setReferencedObject(HasIdAndLocalId hili) {
		setParameter(CLASS_NAME_KEY, hili.getClass().getName());
		setParameter(ID_KEY, hili.getId());
		setParameter(LOCAL_ID_KEY, hili.getLocalId());
	}
}
