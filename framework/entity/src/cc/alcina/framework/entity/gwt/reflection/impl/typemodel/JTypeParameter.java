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

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * <p>
 * Represents one of the type parameters in a generic type.
 *
 * <p>
 * Type can be at least one of: typevariable (A extends Aardvark) - or a class
 * (Aardvark)
 */
public class JTypeParameter extends JClassType
		implements com.google.gwt.core.ext.typeinfo.JTypeParameter {
	private JClassType[] bounds;

	private JGenericType declaringClass;

	private final int ordinal;

	/*
	 * Constructor does not contain declaringClass:JGenericType
	 */
	public JTypeParameter(TypeOracle typeOracle, Type type, int ordinal) {
		super(typeOracle, type);
		if (type instanceof Class) {
			bounds = new JClassType[] { typeOracle.getType(type) };
		} else if (type instanceof TypeVariable) {
			TypeVariable typeVariable = (TypeVariable) type;
			Type[] jdkBounds = typeVariable.getBounds();
			int length = jdkBounds.length;
			bounds = new JClassType[length];
			for (int idx = 0; idx < length; idx++) {
				Type bound = jdkBounds[idx];
				bounds[idx] = typeOracle.getType(bound);
			}
		} else {
			throw new UnsupportedOperationException();
		}
		this.ordinal = ordinal;
	}

	@Override
	public JClassType getBaseType() {
		return bounds[0];
	}

	@Override
	public JClassType[] getBounds() {
		return bounds;
	}

	@Override
	public JGenericType getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public JClassType getErasedType() {
		return getBaseType();
	}

	@Override
	public JClassType getFirstBound() {
		return bounds[0];
	}

	@Override
	public int getOrdinal() {
		return ordinal;
	}

	@Override
	public String getQualifiedSourceName() {
		if (clazz == null) {
			Class simpleBoundType = typeOracle.simpleBoundType(type);
			return getQualifiedSourceName(simpleBoundType);
		} else {
			return super.getQualifiedSourceName();
		}
	}

	@Override
	public String toString() {
		if (type instanceof Class) {
			return super.toString();
		} else if (type instanceof TypeVariable) {
			return type.toString();
		} else {
			throw new UnsupportedOperationException();
		}
	}
}
