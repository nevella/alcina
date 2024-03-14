package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.reflect.Method;

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

public class JAnnotationType extends JRealClassType
		implements com.google.gwt.core.ext.typeinfo.JAnnotationType {
	public JAnnotationType(TypeOracle typeOracle, Class clazz) {
		super(typeOracle, clazz);
	}

	@Override
	public JAnnotationMethod getMethod(String name, JType[] paramTypes)
			throws NotFoundException {
		return (JAnnotationMethod) super.getMethod(name, paramTypes);
	}

	JMethod createMethod(Method m) {
		return new JAnnotationMethod(typeOracle, m);
	}
}
