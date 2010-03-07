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

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class TxtCriterion extends SearchCriterion {
	@ClientInstantiable
	public static enum TxtCriterionType {
		CONTAINS, EQUALS, EQUALS_OR_LIKE
	}

	private String text;

	private TxtCriterionType txtCriterionType = TxtCriterionType.CONTAINS;

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (CommonUtils.isNullOrEmpty(text)) {
			return result;
		}
		switch (txtCriterionType) {
		case EQUALS:
			result.eql = "lower(t." + getTargetPropertyName() + ") =  ? ";
			result.parameters.add(text.toLowerCase());
			break;
		case CONTAINS:
			result.eql = "lower(t." + getTargetPropertyName() + ") like  ? ";
			result.parameters.add("%" + text.toLowerCase() + "%");
			break;
		case EQUALS_OR_LIKE:
			result.eql = "lower(t." + getTargetPropertyName() + ") "
					+ (text.contains("%") ? "like" : "=") + "  ? ";
			result.parameters.add(text.toLowerCase());
			break;
		}
		return result;
	}

	@Override
	public String renderablePropertyName() {
		return "text";
	}

	public void setTxtCriterionType(TxtCriterionType txtCriterionType) {
		this.txtCriterionType = txtCriterionType;
	}

	public TxtCriterionType getTxtCriterionType() {
		return txtCriterionType;
	}
}