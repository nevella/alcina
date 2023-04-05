package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.annotation.Annotation;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

public class JPackage implements com.google.gwt.core.ext.typeinfo.JPackage {
	public JPackage(String pkgName) {
		// FIXME - 1x5
	}

	@Override
	public JClassType findType(String typeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType findType(String[] typeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Annotation[] getAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType getType(String typeName) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JClassType[] getTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean
			isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDefault() {
		// TODO Auto-generated method stub
		return false;
	}
}
