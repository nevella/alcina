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

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection.AccessibleConstructor;

/**
 * Represents a constructor declaration.
 */
public class JConstructor extends JAbstractMethod
		implements com.google.gwt.core.ext.typeinfo.JConstructor, Supplier,
		AccessibleConstructor {
	Constructor constructor;

	public JConstructor(TypeOracle typeOracle, Constructor constructor) {
		super(typeOracle, constructor);
		this.constructor = constructor;
	}

	@Override
	public Object get() {
		try {
			return constructor.newInstance();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	@Override
	public void makeAccessible() {
		constructor.setAccessible(true);
	}
}
