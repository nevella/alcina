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


import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */

 public class EmailAddressValidator implements Validator {
	private boolean ignoreEmpties = true;

	private String multipleSeparator = null;

	private static final String EMAIL_REGEX = "([a-zA-Z0-9_'+*$%\\^&!\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9:]{2,4})+";

	private static final String EMAIL_REGEX_REPLACE = "234IBBDA";

	public Object validate(Object value) throws ValidationException {
		if ((value == null) || (value.toString().length() < 1)) {
			if (ignoreEmpties) {
				return value;
			}
			throw new ValidationException("Not a valid email address",
					EmailAddressValidator.class);
		}
		value=value.toString().trim();
		String sz = value.toString();
		String[] strings = null;
		if (multipleSeparator != null) {
			strings = sz.split(multipleSeparator);
		} else {
			strings = new String[] { sz };
		}
		for (String s : strings) {
			if (!s.replaceAll(EMAIL_REGEX, EMAIL_REGEX_REPLACE).equals(
					EMAIL_REGEX_REPLACE)) {
				throw new ValidationException(CommonUtils.format(
						"'%1' is not a valid email address", s),
						EmailAddressValidator.class);
			}
		}
		return value;
	}

	public void setIgnoreEmpties(boolean ignoreEmpties) {
		this.ignoreEmpties = ignoreEmpties;
	}

	public boolean isIgnoreEmpties() {
		return ignoreEmpties;
	}

	public void setMultipleSeparator(String multipleSeparator) {
		this.multipleSeparator = multipleSeparator;
	}

	public String getMultipleSeparator() {
		return multipleSeparator;
	}
}