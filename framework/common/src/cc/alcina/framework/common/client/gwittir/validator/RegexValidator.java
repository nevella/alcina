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
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.totsp.gwittir.client.validator.ValidationException;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */

 public class RegexValidator implements ParameterisedValidator {
	public static final String PARAM_REGEX = "regex";

	public static final String MULTIPLE_SEPARATOR = "multipleSeparator";

	private String multipleSeparator = null;

	private String regex;

	public String getRegex() {
		return this.regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	private static final String REGEX_REPLACE = "234IBBDA";

	public Object validate(Object value) throws ValidationException {
		if (value == null) {
			return null;
		}
		value = value.toString().trim();
		String sz = value.toString();
		String[] strings = null;
		if (!CommonUtils.isNullOrEmpty(multipleSeparator)) {
			strings = sz.split(multipleSeparator);
		} else {
			strings = new String[] { sz };
		}
		for (String s : strings) {
			if (!s.replaceAll(getRegex(), REGEX_REPLACE).equals(REGEX_REPLACE)) {
				throw new ValidationException(CommonUtils.format(
						"Does not match regex ('%1', '%2')", getRegex(),
						multipleSeparator), RegexValidator.class);
			}
		}
		return value;
	}

	public void setParameters(NamedParameter[] params) {
		NamedParameter p = NamedParameter.Support.getParameter(params,
				PARAM_REGEX);
		regex = p.stringValue();
		p = NamedParameter.Support.getParameter(params, MULTIPLE_SEPARATOR);
		multipleSeparator = p == null ? null : p.stringValue();
	}
}