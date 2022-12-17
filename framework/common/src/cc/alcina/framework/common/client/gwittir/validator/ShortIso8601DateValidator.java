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

import java.util.Date;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
@Reflected
public class ShortIso8601DateValidator implements Validator {
	public static final ShortIso8601DateValidator INSTANCE = new ShortIso8601DateValidator();

	public static final transient String ERR_FMT = "Dates must be "
			+ "entered in the following format: yyyy-mm-dd";

	public static final transient String ERR_INVALID = "The date entered does not exist";

	public ShortIso8601DateValidator() {
	}

	@Override
	@SuppressWarnings("deprecation")
	public Object validate(Object value) throws ValidationException {
		if (value == null || value.toString().length() == 0) {
			return null;
			// throw new ValidationException(ERR_FMT);
		}
		if (value instanceof Date) {
			return value;
		}
		String sValue = value.toString();
		if (!sValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
			throw new ValidationException(ERR_FMT);
		}
		try {
			String[] splits = sValue.split("-");
			Date result = new Date(Integer.parseInt(splits[0]) - 1900,
					Integer.parseInt(splits[1]) - 1,
					Integer.parseInt(splits[2]));
			return result;
		} catch (Exception e) {
			throw new ValidationException(ERR_INVALID);
		}
	}
}
