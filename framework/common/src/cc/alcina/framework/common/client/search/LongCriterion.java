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

import cc.alcina.framework.common.client.util.CommonUtils;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class LongCriterion extends SearchCriterion {
	private String longText;

	public void setLongText(String longText) {
		this.longText = longText;
	}

	public String getLongText() {
		return longText;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (CommonUtils.isNullOrEmpty(longText)) {
			return result;
		}
		try {
			Long l = Long.parseLong(longText);
			result.eql = "t." + getTargetPropertyName() + " =  ? ";
			result.parameters.add(l);
		} catch (Exception e) {
		}
	
		return result;
	}

	@Override
	public String renderablePropertyName() {
		return "longText";
	}
}