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
package cc.alcina.framework.gwt.client.gwittir.renderer;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.HasDisplayName;

/**
 * 
 * @author Nick Reddel
 */
@Reflected
public class IdToStringRenderer extends FlexibleToStringRenderer<HasId> {
	public static final IdToStringRenderer INSTANCE = new IdToStringRenderer();

	public static final IdToStringRenderer BLANK_NULLS_INSTANCE = new IdToStringRenderer(
			true);

	private boolean nullsAsBlanks;

	public IdToStringRenderer() {
	}

	public IdToStringRenderer(boolean nullsAsBlanks) {
		this.nullsAsBlanks = nullsAsBlanks;
	}

	@Override
	public String render(HasId hasId) {
		if (hasId == null) {
			return nullsAsBlanks ? "" : "(Undefined)";
		}
		String dn = HasDisplayName.displayName(hasId);
		String strId = String.valueOf(hasId.getId());
		String toString = hasId.toString();
		if (toString.equals(strId)) {
			return strId;
		}
		if (hasId instanceof Entity
				&& ((Entity) hasId).toLocator().toString().equals(toString)) {
			return strId;
		}
		return hasId.getId() + " : " + toString;
	}
}