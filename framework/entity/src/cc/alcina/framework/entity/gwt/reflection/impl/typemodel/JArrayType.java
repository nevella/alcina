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

import com.google.gwt.core.ext.typeinfo.JType;

/**
 * Type representing a Java array.
 */
public class JArrayType extends JClassType
		implements com.google.gwt.core.ext.typeinfo.JArrayType {
	public JArrayType(TypeOracle typeOracle, Class clazz) {
		super(typeOracle, clazz);
	}

	@Override
	public JType getComponentType() {
		Class c = clazz;
		// FIXME - tm - this erases any parameters in the component type
		while (c.isArray()) {
			c = c.getComponentType();
		}
		return typeOracle.getType(c);
	}

	@Override
	public JClassType getErasedType() {
		return typeOracle.getArrayType(getComponentType().getErasedType());
	}

	@Override
	public int getRank() {
		int rank = 0;
		Class c = clazz;
		while (c.isArray()) {
			c = c.getComponentType();
			rank++;
		}
		return rank;
	}

	@Override
	public JArrayType[] getSubtypes() {
		throw new UnsupportedOperationException();
	}
}
