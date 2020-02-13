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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Window;
import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.TextUtils;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class EmailAddressValidator implements ParameterisedValidator {
	public static final String STANDARD_MULTIPLE_SEPARATOR = "(;|,| )+";

	private static final String EMAIL_REGEX = "([a-zA-Z0-9_'+*$%\\^&!\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9:]{2,8})+";

	private static final String EMAIL_REGEX_REPLACE = "234@@@IBBDA";

	public static final String PARAM_MULTIPLE_SEPARATOR = "multiple-separator";

	public static final String PARAM_IGNORE_EMPTIES = "ignore-empties";

	public static List<String>
			provideAddressesForDefaultSeparator(String addresses) {
		addresses = TextUtils.normalizeWhitespaceAndTrim(addresses);
		return Arrays.asList(addresses.split(STANDARD_MULTIPLE_SEPARATOR))
				.stream().map(s -> TextUtils.normalizeWhitespaceAndTrim(s))
				.collect(Collectors.toList());
	}

	private boolean ignoreEmpties = true;

	private String multipleSeparator = null;

	public String getMultipleSeparator() {
		return multipleSeparator;
	}

	public boolean isIgnoreEmpties() {
		return ignoreEmpties;
	}

	public void setIgnoreEmpties(boolean ignoreEmpties) {
		this.ignoreEmpties = ignoreEmpties;
	}

	public void setMultipleSeparator(String multipleSeparator) {
		this.multipleSeparator = multipleSeparator;
	}

	@Override
	public void setParameters(NamedParameter[] params) {
		NamedParameter p = NamedParameter.Support.getParameter(params,
				PARAM_IGNORE_EMPTIES);
		if (p != null) {
			setIgnoreEmpties(p.booleanValue());
		}
		p = NamedParameter.Support.getParameter(params,
				PARAM_MULTIPLE_SEPARATOR);
		if (p != null) {
			setMultipleSeparator(p.stringValue());
		}
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		if ((value == null) || (value.toString().length() < 1)) {
			if (ignoreEmpties) {
				return value;
			}
			throw new ValidationException("Not a valid email address",
					EmailAddressValidator.class);
		}
		value = value.toString().trim();
		String sz = value.toString();
		String[] strings = null;
		if (multipleSeparator != null) {
			strings = sz.split(multipleSeparator);
		} else {
			strings = new String[] { sz };
		}
		for (String s : strings) {
			String replaceAll = s.replaceAll(EMAIL_REGEX, EMAIL_REGEX_REPLACE);
			boolean overrideOk = false;
			if (GWT.isClient()
					&& Window.Navigator.getUserAgent().indexOf("Edge/") != -1) {
				if (s.contains("@") && s.contains(".")) {
					overrideOk = true;
				}
			}
			if (!(overrideOk || replaceAll.equals(EMAIL_REGEX_REPLACE))) {
				throw new ValidationException(CommonUtils
						.format("'%s' is not a valid email address", s),
						EmailAddressValidator.class);
			}
		}
		return value;
	}

	public EmailAddressValidator withIgnoreEmpties(boolean ignoreEmpties) {
		this.ignoreEmpties = ignoreEmpties;
		return this;
	}
}