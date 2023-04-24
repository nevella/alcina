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

import java.lang.reflect.WildcardType;

import com.google.gwt.core.ext.typeinfo.JGenericType;

/**
 * Wildcard type. FIXME - reflection - typemodel - implement
 */
public class JWildcardType extends JClassType
		implements com.google.gwt.core.ext.typeinfo.JWildcardType {
	public JWildcardType(TypeOracle typeOracle, WildcardType type) {
		super(typeOracle, type);
	}

	@Override
	public JGenericType getBaseType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BoundType getBoundType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType getErasedType() {
		return typeOracle.getJavaLangObject();
	}

	@Override
	public JClassType getFirstBound() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType[] getLowerBounds() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType getUpperBound() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JClassType[] getUpperBounds() {
		throw new UnsupportedOperationException();
	}
}
