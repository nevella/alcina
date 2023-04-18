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

import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.gwt.reflection.reflector.PropertyReflection.ProvidesMethod;

/**
 * Represents a method declaration.
 */
public class JMethod extends JAbstractMethod
		implements com.google.gwt.core.ext.typeinfo.JMethod, ProvidesMethod {
	private Method method;

	public JMethod(TypeOracle typeOracle, Type declaringType, Method method) {
		super(typeOracle, declaringType, method);
		this.method = method;
	}

	@Override
	public String getReadableDeclaration(boolean noAccess, boolean noNative,
			boolean noStatic, boolean noFinal, boolean noAbstract) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JType getReturnType() {
		return typeOracle.resolveType(declaringType,
				method.getGenericReturnType());
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
	public cc.alcina.framework.common.client.reflection.Method provideMethod() {
		return new cc.alcina.framework.common.client.reflection.Method(method,
				new MethodInvokerImpl(method), method.getReturnType());
	}

	@Override
	public String toString() {
		return method.toString();
	}

	static class MethodInvokerImpl<T>
			implements BiFunction<Object, Object[], T> {
		private java.lang.reflect.Method reflectMethod;

		public MethodInvokerImpl(java.lang.reflect.Method reflectMethod) {
			this.reflectMethod = reflectMethod;
		}

		@Override
		public T apply(Object target, Object[] args) {
			try {
				return (T) reflectMethod.invoke(target, args);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}
