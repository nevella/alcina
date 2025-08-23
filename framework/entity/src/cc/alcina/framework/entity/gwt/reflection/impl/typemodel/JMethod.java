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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.gwt.reflection.reflector.PropertyReflection.ProvidesPropertyMethod;

/**
 * Represents a method declaration.
 */
public class JMethod extends JAbstractMethod implements
		com.google.gwt.core.ext.typeinfo.JMethod, ProvidesPropertyMethod {
	protected Method method;

	private JType returnType;

	public JMethod(TypeOracle typeOracle, Method method) {
		super(typeOracle, method);
		this.method = method;
	}

	@Override
	public JMethod clone() {
		JMethod clone = new JMethod(typeOracle, method);
		clone.parameters = parameters.stream().map(JParameter::clone)
				.collect(Collectors.toList());
		return clone;
	}

	@Override
	public String getReadableDeclaration(boolean noAccess, boolean noNative,
			boolean noStatic, boolean noFinal, boolean noAbstract) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JType getReturnType() {
		if (returnType == null) {
			Type genericReturnType = method.getGenericReturnType();
			returnType = typeOracle.getType(genericReturnType);
		}
		return returnType;
	}

	@Override
	public boolean isAbstract() {
		return Modifier.isAbstract(modifierBits);
	}

	@Override
	public boolean isFinal() {
		return Modifier.isFinal(modifierBits);
	}

	@Override
	public JMethod isMethod() {
		return this;
	}

	@Override
	public boolean isNative() {
		return Modifier.isNative(modifierBits);
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(modifierBits);
	}

	@Override
	public cc.alcina.framework.common.client.reflection.Method
			providePropertyMethod(boolean getter,
					// ignored for JMethod accessor (the method itself should
					// fire changes)
					boolean firePropertyChangeEvents) {
		return new cc.alcina.framework.common.client.reflection.Method(method,
				new MethodInvokerImpl(method), method.getReturnType());
	}

	public void setReturnType(JType returnType) {
		this.returnType = returnType;
	}

	@Override
	public String toString() {
		return method.toString();
	}

	static class MethodInvokerImpl<T>
			implements BiFunction<Object, Object[], T> {
		private java.lang.reflect.Method reflectMethod;

		public MethodInvokerImpl(java.lang.reflect.Method reflectMethod) {
			reflectMethod.setAccessible(true);
			this.reflectMethod = reflectMethod;
		}

		@Override
		public T apply(Object target, Object[] args) {
			try {
				return (T) reflectMethod.invoke(target, args);
			} catch (Throwable e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}
