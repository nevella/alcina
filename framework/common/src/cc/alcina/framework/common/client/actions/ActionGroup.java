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

package cc.alcina.framework.common.client.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Nick Reddel
 */

 public class ActionGroup {
	public List<VetoableAction> actions = new ArrayList<VetoableAction>();

	private final boolean rightAligned;

	public ActionGroup(VetoableAction[] actions) {
		this(actions, false);
	}

	public ActionGroup(VetoableAction[] actions, boolean rightAligned) {
		this.rightAligned = rightAligned;
		this.actions.addAll(Arrays.asList(actions));
	}

	public boolean isRightAligned() {
		return rightAligned;
	}
}