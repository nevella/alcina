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

import java.util.HashMap;
import java.util.Map;

import com.totsp.gwittir.client.validator.Validator;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public interface HasValidators {
	public Validator validator(String propertyName);
	public static class ValidatorSupport{
		Map<String, Validator> validatorMap = new HashMap<String, Validator>();
		private boolean initialised;
		public ValidatorSupport addValidator(String propertyName, Validator validator){
			validatorMap.put(propertyName, validator);
			
			return this;
		}
		public Validator validator(String propertyName){
			return validatorMap.get(propertyName);
		}
		public void setInitialised(boolean initialised) {
			this.initialised = initialised;
		}
		public boolean isInitialised() {
			return initialised;
		}
	}
}
