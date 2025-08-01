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

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

@Reflected
public class NotBlankValidator implements Validator {
	public static LooseContext.Key<Boolean> CONTEXT_IGNORE = LooseContext
			.key(NotBlankValidator.class, "CONTEXT_IGNORE");

	@Override
	public Object validate(Object value) throws ValidationException {
		if (value == null || value.toString().isEmpty()) {
			if (!CONTEXT_IGNORE.is()) {
				throw new ValidationException("Required",
						NotBlankValidator.class);
			}
		}
		return value;
	}
}