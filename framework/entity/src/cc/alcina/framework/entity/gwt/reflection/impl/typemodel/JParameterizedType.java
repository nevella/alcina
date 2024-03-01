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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gwt.core.ext.typeinfo.JGenericType;

/**
 * Type declaration that has type parameters.
 */
public class JParameterizedType extends JClassType<ParameterizedType>
		implements com.google.gwt.core.ext.typeinfo.JParameterizedType {
	private JClassType[] typeArguments;

	public JParameterizedType(TypeOracle typeOracle, ParameterizedType type) {
		super(typeOracle, type);
	}

	@Override
	public JGenericType getBaseType() {
		Type rawType = type.getRawType();
		if (rawType instanceof JGenericType) {
			return (JGenericType) typeOracle.getType(rawType);
		} else {
			return null;
		}
	}

	@Override
	public JClassType getErasedType() {
		return typeOracle.getType(type.getRawType());
	}

	@Override
	public JClassType getRawType() {
		return getErasedType();
	}

	@Override
	public JClassType[] getTypeArgs() {
		return typeArguments;
	}

	public JClassType[] getTypeArguments() {
		return this.typeArguments;
	}

	public void setTypeArguments(JClassType[] typeArguments) {
		this.typeArguments = typeArguments;
	}
}
