/*
 * IntegerValidator.java
 *
 * Created on April 12, 2007, 5:38 PM
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

import cc.alcina.framework.common.client.gwittir.validator.DoubleValidator;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public class IntegerValidator implements Validator {
	public static final IntegerValidator INSTANCE = new IntegerValidator();

	/** Creates a new instance of IntegerValidator */
	protected IntegerValidator() {
	}

	protected boolean allowNull() {
		return true;
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		if (value == null) {
			if (!allowNull()) {
				throw new ValidationException("Value is required",
						DoubleValidator.class);
			}
			return value;
		}
		if (value instanceof Integer) {
			return value;
		}
		Integer i;
		try {
			i = Integer.valueOf(value.toString());
		} catch (NumberFormatException nfe) {
			throw new ValidationException("Must be an integer value.",
					IntegerValidator.class);
		}
		return i;
	}

	public static class Primitive extends IntegerValidator {
		@Override
		protected boolean allowNull() {
			return false;
		}
	}
}
