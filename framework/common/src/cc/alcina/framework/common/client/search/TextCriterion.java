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
package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/**
 * @author Nick Reddel
 */
@SearchDefinitionSerializationInfo("tx")
@TypeSerialization("text")
@Registration(SearchDefinitionSerializationInfo.class)
public class TextCriterion extends SearchCriterion implements HasValue<String> {
	private String value;

	private TextCriterionType textCriterionType = TextCriterionType.CONTAINS;

	public TextCriterion() {
		setOperator(StandardSearchOperator.CONTAINS);
	}

	public TextCriterion(String text) {
		this();
		setValue(text);
	}

	@Override
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (CommonUtils.isNullOrEmpty(value)) {
			return result;
		}
		switch (textCriterionType) {
		case EQUALS:
			result.eql = "lower(" + targetPropertyNameWithTable() + ") =  ? ";
			result.parameters.add(value.toLowerCase());
			break;
		case CONTAINS:
			result.eql = "lower(" + targetPropertyNameWithTable()
					+ ") like  ? ";
			result.parameters.add("%" + value.toLowerCase() + "%");
			break;
		case EQUALS_OR_LIKE:
			result.eql = "lower(" + targetPropertyNameWithTable() + ") "
					+ (value.contains("%") ? "like" : "=") + "  ? ";
			result.parameters.add(value.toLowerCase());
			break;
		}
		return result;
	}

	public TextCriterionType getTextCriterionType() {
		return textCriterionType;
	}

	@Override
	@PropertySerialization(defaultProperty = true)
	public String getValue() {
		return value;
	}

	public void setTextCriterionType(TextCriterionType textCriterionType) {
		this.textCriterionType = textCriterionType;
	}

	@Override
	public void setValue(String value) {
		String old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	@Override
	public String toString() {
		String string = CommonUtils.nullToEmpty(getValue());
		if (string.length() > 0
				&& getOperator() != StandardSearchOperator.CONTAINS) {
			string = Ax.format("%s '%s'", Ax.friendly(getOperator()), string);
		}
		return string.length() == 0 ? ""
				: Ax.isBlank(getDisplayName()) ? Ax.format("\"%s\"", string)
						: getDisplayName() + ": " + string;
	}

	public TextCriterion withValue(String text) {
		setValue(text);
		return this;
	}

	@Reflected
	public static enum TextCriterionType {
		CONTAINS, EQUALS, EQUALS_OR_LIKE
	}
}
