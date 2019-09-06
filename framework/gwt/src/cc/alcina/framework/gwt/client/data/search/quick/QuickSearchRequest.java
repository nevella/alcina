package cc.alcina.framework.gwt.client.data.search.quick;

import java.io.Serializable;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.gwt.client.data.entity.DataDomainBase;

public class QuickSearchRequest implements Serializable {
	public String text;

	public String className;

	public Class<? extends DataDomainBase> provideType() {
		return Reflections.classLookup().getClassForName(className);
	}

	public void putType(Class<? extends DataDomainBase> type) {
		className = type.getName();
	}
}
