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
import java.util.function.BiFunction;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JType;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.Method;
import cc.alcina.framework.entity.gwt.reflection.reflector.PropertyReflection.ProvidesPropertyMethod;

/**
 * Represents a field declaration.
 */
public class JField implements com.google.gwt.core.ext.typeinfo.JField,
		ProvidesPropertyMethod {
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

	@Override
	public Method providePropertyMethod(boolean getter,
			boolean firePropertyChangeEvents) {
		return new cc.alcina.framework.common.client.reflection.Method(field,
				new MethodInvokerImpl(field, getter, firePropertyChangeEvents),
				field.getType());
	}

	public void setType(JType type) {
		this.type = type;
	}

	static class MethodInvokerImpl<T>
			implements BiFunction<Object, Object[], T> {
		java.lang.reflect.Field reflectField;

		boolean getter;

		boolean firePropertyChangeEvents;

		MethodInvokerImpl(java.lang.reflect.Field reflectField, boolean getter,
				boolean firePropertyChangeEvents) {
			this.reflectField = reflectField;
			this.getter = getter;
			this.firePropertyChangeEvents = firePropertyChangeEvents;
		}

		@Override
		public T apply(Object target, Object[] args) {
			try {
				if (getter) {
					return (T) reflectField.get(target);
				} else {
					Object value = args[0];
					if (firePropertyChangeEvents) {
						Object oldValue = reflectField.get(target);
						reflectField.set(target, value);
						SourcesPropertyChangeEvents eventSource = (SourcesPropertyChangeEvents) target;
						eventSource.firePropertyChange(reflectField.getName(),
								oldValue, value);
					} else {
						reflectField.set(target, value);
					}
					return null;
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}
