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


import com.totsp.gwittir.client.validator.StringLengthValidator;
import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */

 public class ParameterisedStringLengthValidator implements ParameterisedValidator{
	public static final String MAX_CHARS = "maxChars";
	public static final String MIN_CHARS = "minChars";
	int max=9999;
    int min;

    /** Creates a new instance of StringLengthValidator */
    public ParameterisedStringLengthValidator() {
       
    }

    public Object validate(Object value) throws ValidationException {
        if((value == null) || (value.toString().length() < min)) {
            throw new ValidationException("Value must be at least " + min +
                " and no more than " + max + " characters.",
                StringLengthValidator.class);
        }

        return value;
    }

	public void setParameters(NamedParameter[] params) {
		NamedParameter minP = NamedParameter.Support.getParameter(params, MIN_CHARS);
		if (minP!=null){
			min=minP.intValue();
		}
		NamedParameter maxP = NamedParameter.Support.getParameter(params, MAX_CHARS);
		if (maxP!=null){
			max=maxP.intValue();
		}
	}
}
