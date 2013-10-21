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

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

/**
 * 
 * @author nick@alcina.cc
 *
 */
@ClientInstantiable
public class ShortDateValidator implements Validator {
	public static final ShortDateValidator INSTANCE = new ShortDateValidator();

	public ShortDateValidator() {
	}

	public static final transient String ERR_FMT = "Dates must be "
			+ "entered in the following format: dd/mm/yyyy";

	public static final transient String ERR_INVALID = "The date entered does not exist";

	@SuppressWarnings("deprecation")
	public Object validate(Object value) throws ValidationException {
		if (value==null||value.toString().length()==0) {
			return null;
//			throw new ValidationException(ERR_FMT);
		}
		String sValue = value.toString();
		String[] splits = sValue.split("/");
		if (splits.length != 3) {
			throw new ValidationException(ERR_FMT);
		}
		try {
			Date result = new Date(Integer.parseInt(splits[2]) - 1900, Integer
					.parseInt(splits[1]) - 1, Integer.parseInt(splits[0]));
			return result;
		} catch (Exception e) {
			throw new ValidationException(ERR_INVALID);
		}
	}
}
