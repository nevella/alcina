/*
 * DoubleValidator.java
 *
 * Created on July 16, 2007, 5:37 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package cc.alcina.framework.common.client.gwittir.validator;

import java.util.Date;

import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;

/**
 *
 */
@Reflected
public class DateValidator extends RegexValidator {
	public DateValidator() {
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		if (value == null) {
			return value;
		}
		String validatable = value.toString();
		if (value instanceof Date) {
			validatable = Ax.dateSlash((Date) value);
		}
		super.validate(validatable);
		return value;
	}
}
