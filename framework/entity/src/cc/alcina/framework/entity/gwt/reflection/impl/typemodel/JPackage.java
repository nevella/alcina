package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.annotation.Annotation;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

public class JPackage implements com.google.gwt.core.ext.typeinfo.JPackage {
	private String pkgName;

	public JPackage(String pkgName) {
		this.pkgName = pkgName;
		// FIXME - 1x5
	}

	@Override
	public JClassType findType(String typeName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType findType(String[] typeName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Annotation[] getAnnotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return pkgName;
	}

	@Override
	public JClassType getType(String typeName) throws NotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType[] getTypes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean
			isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDefault() {
		throw new UnsupportedOperationException();
	}
}
