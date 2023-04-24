package cc.alcina.framework.entity.gwt.reflection.reflector;

import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.ProvidesJavaType;

abstract class ReflectionElement {
	public abstract void prepare();

	Class asJavaType(JClassType jClassType) {
		return ((ProvidesJavaType) jClassType).provideJavaType();
	}
}
