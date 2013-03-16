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
 * @author Nick Reddel
 */
public class TxtCriterion extends SearchCriterion {
	static final transient long serialVersionUID = -2L;

	@ClientInstantiable
	public static enum TxtCriterionType {
		CONTAINS, EQUALS, EQUALS_OR_LIKE
	}

	private String text;

	private TxtCriterionType txtCriterionType = TxtCriterionType.CONTAINS;

	public TxtCriterion() {
	}

	public TxtCriterion(String displayName) {
		super(displayName);
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public boolean equivalentTo(SearchCriterion other) {
		if (other instanceof TxtCriterion) {
			TxtCriterion otherT = (TxtCriterion) other;
			return otherT.getDirection() == getDirection()
					&& otherT.getTxtCriterionType() == getTxtCriterionType()
					&& CommonUtils.equalsWithNullEquality(getText(),
							otherT.getText());
		}
		return false;
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

	public void setTxtCriterionType(TxtCriterionType txtCriterionType) {
		this.txtCriterionType = txtCriterionType;
	}

	public TxtCriterionType getTxtCriterionType() {
		return txtCriterionType;
	}

	@Override
	public String toString() {
		String string = CommonUtils.nullToEmpty(getText());
		return string.length() == 0 ? "" : getDisplayName() + ": " + string;
	}

	@Override
	protected TxtCriterion copyProperties(SearchCriterion searchCriterion) {
		TxtCriterion sc = (TxtCriterion) searchCriterion;
		sc.text = text;
		sc.txtCriterionType = txtCriterionType;
		return super.copyProperties(sc);
	}
	@Override
	public TxtCriterion clone() throws CloneNotSupportedException {
		TxtCriterion copy = new TxtCriterion();
		copy.copyProperties(this);
		return copy;
	}
}