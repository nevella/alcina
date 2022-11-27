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
package cc.alcina.framework.common.client.actions.instances;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.provider.TextProvider;

/**
 *
 * @author Nick Reddel
 */
public class OkAction extends PermissibleAction {
	public static final OkAction INSTANCE = new OkAction();

	private OkAction() {
	}

	@Override
	public String getActionName() {
		return TextProvider.get().getUiObjectText(getClass(),
				TextProvider.DISPLAY_NAME, "OK");
	}
}
