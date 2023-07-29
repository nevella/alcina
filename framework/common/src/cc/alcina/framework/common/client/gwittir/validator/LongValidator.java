/*
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
package cc.alcina.framework.common.client.gwittir.validator;

import com.totsp.gwittir.client.validator.DoubleValidator;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 * 
 * 
 */
@Reflected
public class LongValidator implements Validator {
	public static final LongValidator INSTANCE = new LongValidator();

	public LongValidator() {
	}

	protected boolean allowNull() {
		return true;
	}

	public static class Primitive extends LongValidator {
		@Override
		protected boolean allowNull() {
			return false;
		}
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
		if (value instanceof Long) {
			return value;
		}
		Long l;
		try {
			l = Long.valueOf(value.toString());
		} catch (NumberFormatException nfe) {
			throw new ValidationException("Must be an integer value.",
					LongValidator.class);
		}
		return l;
	}
}
