/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * Represents a field declaration.
 */
public class JField implements com.google.gwt.core.ext.typeinfo.JField {
	private TypeOracle typeOracle;

	private Field field;

	private int modifierBits;

	private JType type;

	public JField(TypeOracle typeOracle, Field field) {
		this.typeOracle = typeOracle;
		this.field = field;
		modifierBits = field.getModifiers();
	}

	@Override
	public JField clone() {
		return new JField(typeOracle, field);
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return field.getAnnotation(annotationClass);
	}

	@Override
	public Annotation[] getAnnotations() {
		return field.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType getEnclosingType() {
		return typeOracle.getType(field.getDeclaringClass());
	}

	@Override
	public String getName() {
		return field.getName();
	}

	@Override
	public JType getType() {
		if (type == null) {
			type = typeOracle.getType(field.getGenericType());
		}
		return type;
	}

	@Override
	public boolean
			isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return getAnnotation(annotationClass) != null;
	}

	@Override
	public boolean isDefaultAccess() {
		return !(Modifier.isPrivate(modifierBits)
				|| Modifier.isPublic(modifierBits)
				|| Modifier.isProtected(modifierBits));
	}

	@Override
	public JEnumConstant isEnumConstant() {
		return null;
	}

	@Override
	public boolean isFinal() {
		return Modifier.isFinal(modifierBits);
	}

	@Override
	public boolean isPrivate() {
		return Modifier.isPrivate(modifierBits);
	}

	@Override
	public boolean isProtected() {
		return Modifier.isProtected(modifierBits);
	}

	@Override
	public boolean isPublic() {
		return Modifier.isPublic(modifierBits);
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(modifierBits);
	}

	@Override
	public boolean isTransient() {
		return Modifier.isTransient(modifierBits);
	}

	@Override
	public boolean isVolatile() {
		return Modifier.isVolatile(modifierBits);
	}

	public void setType(JType type) {
		this.type = type;
	}
}
