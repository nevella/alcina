package cc.alcina.framework.gwt.client.entity.search.quick;

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.reflection.Reflections;

@Bean
public class QuickSearchRequest implements Serializable {
	private String text;

	private String className;

	public String getClassName() {
		return className;
	}

	public String getText() {
		return text;
	}

	public Class<? extends VersionableEntity> provideType() {
		return Reflections.forName(className);
	}

	public void putType(Class<? extends VersionableEntity> type) {
		className = type.getName();
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void setText(String text) {
		this.text = text;
	}
}
