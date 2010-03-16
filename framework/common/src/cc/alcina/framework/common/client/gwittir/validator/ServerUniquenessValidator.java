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


import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;

import com.totsp.gwittir.client.validator.ValidationException;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */

 public class ServerUniquenessValidator extends ServerValidator {
	public static final transient String OBJECT_CLASS = "objectClass";

	public static final transient String PROPERTY_NAME = "propertyName";

	public static final transient String VALUE_TEMPLATE = "valueTemplate";

	public static final String CASE_INSENSITIVE = "caseInsensitive";

	private transient Class objectClass;

	private String objectClassName;

	private String propertyName;

	private String valueTemplate;

	private String value;

	private String suggestedValue;

	private boolean caseInsensitive;

	public transient String originalValue;

	private Long okId;

	public ServerUniquenessValidator() {
		initProperties();
	}

	public Class getObjectClass() {
		if (objectClass == null && objectClassName != null) {
			objectClass = CommonLocator.get().classLookup().getClassForName(
					objectClassName);
		}
		return objectClass;
	}

	public Long getOkId() {
		return okId;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public String getSuggestedValue() {
		return suggestedValue;
	}

	public String getValue() {
		return value;
	}

	public String getValueTemplate() {
		return this.valueTemplate;
	}

	public boolean isCaseInsensitive() {
		return caseInsensitive;
	}

	public void setCaseInsensitive(boolean caseInsensitive) {
		this.caseInsensitive = caseInsensitive;
	}

	public void setObjectClass(Class objectClass) {
		this.objectClass = objectClass;
		if (objectClass != null) {
			objectClassName = objectClass.getName();
		}
	}

	public void setOkId(Long okId) {
		this.okId = okId;
	}

	@Override
	public void setParameters(NamedParameter[] parameters) {
		NamedParameter parameter = NamedParameter.Support.getParameter(
				parameters, OBJECT_CLASS);
		if (parameter != null) {
			setObjectClass(parameter.classValue());
		}
		parameter = NamedParameter.Support.getParameter(parameters,
				PROPERTY_NAME);
		if (parameter != null) {
			setPropertyName(parameter.stringValue());
		}
		parameter = NamedParameter.Support.getParameter(parameters,
				VALUE_TEMPLATE);
		if (parameter != null) {
			setValueTemplate(parameter.stringValue());
		}
		parameter = NamedParameter.Support.getParameter(parameters,
				CASE_INSENSITIVE);
		if (parameter != null) {
			setCaseInsensitive(parameter.booleanValue());
		}
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setSuggestedValue(String suggestedValue) {
		this.suggestedValue = suggestedValue;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setValueTemplate(String valueTemplate) {
		this.valueTemplate = valueTemplate;
	}

	@Override
	public Object validate(Object value) throws ValidationException {
		setValue(value == null ? null : value.toString());
		return super.validate(value);
	}

	// this is for hard-coded property-defined subclasses
	protected void initProperties() {
	}
}
