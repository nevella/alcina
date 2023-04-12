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
import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gwt.core.ext.typeinfo.JAnnotationMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;

/**
 * Represents a method or constructor declaration.
 */
public class JAbstractMethod
		implements com.google.gwt.core.ext.typeinfo.JAbstractMethod {
	protected TypeOracle typeOracle;

	protected Executable executable;

	protected int modifierBits;

	protected List<JParameter> parameters;

	JAbstractMethod(TypeOracle typeOracle, Executable executable) {
		this.typeOracle = typeOracle;
		this.executable = executable;
		this.modifierBits = executable.getModifiers();
		this.parameters = Arrays.stream(executable.getParameters())
				.map(p -> new JParameter(typeOracle, this, p))
				.collect(Collectors.toList());
	}

	@Override
	public JParameter findParameter(String name) {
		return parameters.stream()
				.filter(p -> Objects.equals(p.getName(), name)).findFirst()
				.orElse(null);
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return executable.getAnnotation(annotationClass);
	}

	@Override
	public Annotation[] getAnnotations() {
		return executable.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType getEnclosingType() {
		return typeOracle.getType(executable.getDeclaringClass());
	}

	@Override
	public JType[] getErasedParameterTypes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getJsniSignature() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return executable.getName();
	}

	@Override
	public JParameter[] getParameters() {
		return parameters.toArray(new JParameter[parameters.size()]);
	}

	@Override
	public JType[] getParameterTypes() {
		List<JType> types = parameters.stream().map(JParameter::getType)
				.collect(Collectors.toList());
		return types.toArray(new JType[types.size()]);
	}

	@Override
	public String getReadableDeclaration() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType[] getThrows() {
		List<JClassType> types = Arrays.stream(executable.getExceptionTypes())
				.map(typeOracle::getType).collect(Collectors.toList());
		return types.toArray(new JClassType[types.size()]);
	}

	@Override
	public JTypeParameter[] getTypeParameters() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JAnnotationMethod isAnnotationMethod() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean
			isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return getAnnotation(annotationClass) != null;
	}

	@Override
	public JConstructor isConstructor() {
		return null;
	}

	@Override
	public boolean isDefaultAccess() {
		return !(Modifier.isPrivate(modifierBits)
				|| Modifier.isPublic(modifierBits)
				|| Modifier.isProtected(modifierBits));
	}

	@Override
	public JMethod isMethod() {
		return null;
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
	public boolean isVarArgs() {
		throw new UnsupportedOperationException();
	}
}
