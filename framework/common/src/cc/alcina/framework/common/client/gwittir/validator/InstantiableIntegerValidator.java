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

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */

 public class InstantiableIntegerValidator implements Validator {
	/** Creates a new instance of IntegerValidator */
	public InstantiableIntegerValidator() {
	}

	public Object validate(Object value) throws ValidationException {
		if (value == null || value instanceof Integer) {
			return value;
		}
		Integer i;
		try {
			i = Integer.valueOf(value.toString());
		} catch (NumberFormatException nfe) {
			throw new ValidationException("Must be an integer value.",
					InstantiableIntegerValidator.class);
		}
		return i;
	}
}
