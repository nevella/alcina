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

import java.util.Collection;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;

/**
 * 
 * @author Nick Reddel
 */
public class UniqueInCollectionValidator implements Validator {
	private final Collection c;

	private final String propertyName;

	private final Object sourceObject;

	public UniqueInCollectionValidator(Collection c, String propertyName,
			Object sourceObject) {
		this.c = c;
		this.propertyName = propertyName;
		this.sourceObject = sourceObject;
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		if (value == null) {
			return value;
		}
		for (Object o : c) {
			if (o != sourceObject && value.equals(Reflections.propertyAccessor()
					.getPropertyValue(o, propertyName))) {
				if (o instanceof Entity && sourceObject instanceof Entity) {
					if (EntityHelper.equals((Entity) o,
							(Entity) sourceObject)) {
						continue;
					}
				}
				throw new ValidationException("Value must be unique",
						UniqueInCollectionValidator.class);
			}
		}
		return value;
	}
}