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

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class IntRangeValidator implements ParameterisedValidator {
	public static final String PARAM_RANGE = "PARAM_RANGE";

	IntPair range;

	/** Creates a new instance of IntegerValidator */
	public IntRangeValidator() {
	}

	@Override
	public void setParameters(NamedParameter[] params) {
		NamedParameter p = NamedParameter.Support.getParameter(params,
				PARAM_RANGE);
		range = IntPair.parseIntPair(p.stringValue());
	}

	public Object validate(Object value) throws ValidationException {
		if (value == null) {
			throw new ValidationException("Value is required",
					IntRangeValidator.class);
		}
		Integer i;
		if (value instanceof Integer) {
			i = (Integer) value;
		}
		try {
			i = Integer.valueOf(value.toString());
		} catch (NumberFormatException nfe) {
			throw new ValidationException("Must be an integer value.",
					IntRangeValidator.class);
		}
		if (!range.contains(i)) {
			throw new ValidationException(
					CommonUtils.formatJ("Must be in the range %s", range),
					IntRangeValidator.class);
		}
		return i;
	}
}
