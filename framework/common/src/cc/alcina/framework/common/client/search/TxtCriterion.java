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

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
@SearchDefinitionSerializationInfo("tx")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class TxtCriterion extends SearchCriterion implements HasValue<String> {
	static final transient long serialVersionUID = -2L;

	private String text;

	private TxtCriterionType txtCriterionType = TxtCriterionType.CONTAINS;

	public TxtCriterion() {
	}

	public TxtCriterion(String text) {
		super();
		setText(text);
	}

	@Override
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (CommonUtils.isNullOrEmpty(text)) {
			return result;
		}
		switch (txtCriterionType) {
		case EQUALS:
			result.eql = "lower(" + targetPropertyNameWithTable() + ") =  ? ";
			result.parameters.add(text.toLowerCase());
			break;
		case CONTAINS:
			result.eql = "lower(" + targetPropertyNameWithTable()
					+ ") like  ? ";
			result.parameters.add("%" + text.toLowerCase() + "%");
			break;
		case EQUALS_OR_LIKE:
			result.eql = "lower(" + targetPropertyNameWithTable() + ") "
					+ (text.contains("%") ? "like" : "=") + "  ? ";
			result.parameters.add(text.toLowerCase());
			break;
		}
		return result;
	}

	public String getText() {
		return text;
	}

	public TxtCriterionType getTxtCriterionType() {
		return txtCriterionType;
	}

	@Override
	@XmlTransient
	@AlcinaTransient
	public String getValue() {
		return getText();
	}

	public void setText(String text) {
		String old_text = this.text;
		this.text = text;
		propertyChangeSupport().firePropertyChange("text", old_text, text);
	}

	public void setTxtCriterionType(TxtCriterionType txtCriterionType) {
		this.txtCriterionType = txtCriterionType;
	}

	@Override
	public void setValue(String text) {
		setText(text);
	}

	@Override
	public String toString() {
		String string = CommonUtils.nullToEmpty(getText());
		return string.length() == 0 ? ""
				: Ax.isBlank(getDisplayName()) ? Ax.format("\"%s\"", string)
						: getDisplayName() + ": " + string;
	}

	public TxtCriterion withValue(String text) {
		setText(text);
		return this;
	}

	@ClientInstantiable
	public static enum TxtCriterionType {
		CONTAINS, EQUALS, EQUALS_OR_LIKE
	}
}
