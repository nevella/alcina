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
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.common.client.util.Ax;

/**
 * Represents a parameter in a declaration.
 */
public class JParameter implements com.google.gwt.core.ext.typeinfo.JParameter {
	private TypeOracle typeOracle;

	private JAbstractMethod jMethod;

	private Parameter parameter;

	private JType type;

	public JParameter(TypeOracle typeOracle, JAbstractMethod jMethod,
			Parameter parameter) {
		this.typeOracle = typeOracle;
		this.jMethod = jMethod;
		this.parameter = parameter;
	}

	@Override
	public JParameter clone() {
		return new JParameter(typeOracle, jMethod, parameter);
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return parameter.getAnnotation(annotationClass);
	}

	@Override
	public Annotation[] getAnnotations() {
		return parameter.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JAbstractMethod getEnclosingMethod() {
		return jMethod;
	}

	@Override
	public String getName() {
		return parameter.getName();
	}

	@Override
	public JType getType() {
		if (type == null) {
			Type parameterizedType = parameter.getParameterizedType();
			type = typeOracle.getType(parameterizedType);
		}
		return type;
	}

	@Override
	public boolean
			isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return getAnnotation(annotationClass) == null;
	}

	public void setType(JType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return Ax.format("[%s] :: %s", parameter, jMethod);
	}
}
