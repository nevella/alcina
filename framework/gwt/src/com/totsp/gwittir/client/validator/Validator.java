/*
 * Validator.java
 *
 * Created on April 12, 2007, 5:36 PM
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.totsp.gwittir.client.validator;

import java.util.function.Function;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.Property;

/**
 *
 * 
 * <p>
 * NR - note, a validator is really a "converter plus" - particularly because
 * validators can form chains
 * <p>
 * FIXME - alcina - In fact, it makes more sense for converter to subclass
 * validator - it's an always-succeeding validator basically (and validator
 * should be typed)
 * 
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public interface Validator extends Function {
	default Object apply(Object value) {
		try {
			return validate(value);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public Object validate(Object value) throws ValidationException;

	public interface Has {
		Validator getValidator();
	}

	public interface Bidi extends Validator {
		Validator inverseValidator();
	}

	/**
	 * A noop implementation for sync validators, this allows async validation
	 * to exit the sync validation process and reenter once complete
	 * 
	 * @param value
	 *            the value to validate
	 * @param postAsyncValidation
	 *            the runnable to execute once async validation is complete
	 * @return true if async validation is required
	 */
	default boolean validateAsync(Object value, Runnable postAsyncValidation) {
		return false;
	}

	/**
	 * Makes the defining property available to the validator for configuration
	 * from property annotations
	 * 
	 * @param property
	 */
	default void onProperty(Property property) {
	}
}
