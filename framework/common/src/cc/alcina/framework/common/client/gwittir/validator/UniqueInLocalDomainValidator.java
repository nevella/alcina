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

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;

/**
 * 
 * @author Nick Reddel
 */
@ClientInstantiable
public class UniqueInLocalDomainValidator
		implements ParameterisedValidator, RequiresSourceValidator {
	public static final String OBJECT_CLASS = "object_class";

	public static final String PROPERTY_NAME = "property_name";

	private Class domainClass;

	private String propertyName;

	private Entity sourceObject;

	public UniqueInLocalDomainValidator() {
	}

	public void setParameters(NamedParameter[] params) {
		NamedParameter p = NamedParameter.Support.getParameter(params,
				OBJECT_CLASS);
		this.domainClass = p.classValue();
		p = NamedParameter.Support.getParameter(params, PROPERTY_NAME);
		this.propertyName = p.stringValue();
	}

	public void setSourceObject(Entity sourceObject) {
		this.sourceObject = sourceObject;
	}

	public Object validate(Object value) throws ValidationException {
		if (value == null) {
			return value;
		}
		Collection<Entity> c = TransformManager.get()
				.getCollection(domainClass);
		for (Entity entity : c) {
			if (EntityHelper.equals(sourceObject, entity)) {
				continue;
			}
			if (!(value.equals(entity)) && value.equals(Reflections
					.propertyAccessor().getPropertyValue(entity, propertyName))) {
				throw new ValidationException("Value must be unique",
						UniqueInLocalDomainValidator.class);
			}
		}
		return value;
	}
}