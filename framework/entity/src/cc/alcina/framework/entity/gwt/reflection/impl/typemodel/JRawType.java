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

import com.google.gwt.core.ext.typeinfo.JParameterizedType;

/**
 * Type declaration that has type parameters.
 */
public class JRawType extends JClassType
		implements com.google.gwt.core.ext.typeinfo.JRawType {
	private JGenericType base;

	public JRawType(JGenericType jGenericType) {
		super(jGenericType.typeOracle, jGenericType.clazz);
		this.base = jGenericType;
	}

	@Override
	public JParameterizedType asParameterizedByWildcards() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JGenericType getBaseType() {
		return base;
	}

	@Override
	public JClassType getErasedType() {
		return base.getErasedType();
	}

	@Override
	public JGenericType getGenericType() {
		return base;
	}
}
