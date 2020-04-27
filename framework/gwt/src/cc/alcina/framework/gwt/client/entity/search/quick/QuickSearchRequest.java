package cc.alcina.framework.gwt.client.entity.search.quick;

import java.io.Serializable;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;

public class QuickSearchRequest implements Serializable {
	public String text;

	public String className;

	public Class<? extends VersionableEntity> provideType() {
		return Reflections.classLookup().getClassForName(className);
	}

	public void putType(Class<? extends VersionableEntity> type) {
		className = type.getName();
	}
}
