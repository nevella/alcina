package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.reflect.Method;

public class JAnnotationMethod extends JMethod
		implements com.google.gwt.core.ext.typeinfo.JAnnotationMethod {
	public JAnnotationMethod(TypeOracle typeOracle, Method method) {
		super(typeOracle, method);
	}

	@Override
	public Object getDefaultValue() {
		return method.getDefaultValue();
	}
}
