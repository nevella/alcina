/*
 * NotNullValidator.java
 *
 * Created on July 16, 2007, 5:16 PM
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
package cc.alcina.framework.common.client.gwittir.validator;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a> Modified - Nick Reddel, added ClientInstantiable
 *         annotation for framework support
 */
@Reflected
public class NotNullValidator implements Validator.Bidi {
	public static final NotNullValidator INSTANCE = new NotNullValidator();

	/** Creates a new instance of NotNullValidator */
	public NotNullValidator() {
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		if (value == null
				|| (value instanceof String && value.toString().isEmpty())) {
			throw new ValidationException("Value cannot be empty.",
					NotNullValidator.class);
		}
		return value;
	}

	class Passthrough implements Validator.Bidi {
		@Override
		public Object validate(Object value) {
			return value;
		}

		@Override
		public Validator inverseValidator() {
			return NotNullValidator.this;
		}
	}

	@Override
	public Validator inverseValidator() {
		return new Passthrough();
	}
}
