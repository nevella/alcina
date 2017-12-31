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
package cc.alcina.framework.common.client.logic.permissions.rules;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.PermissionsExtensionForRule;

public class PermittedDuringCreationRequest
		extends PermissionsExtensionForRule {
	public static final String RULE_NAME = "PermittedDuringCreationRequest";

	public String getRuleName() {
		return RULE_NAME;
	}

	public Boolean isPermitted(Object o, Permissible p) {
		return TransformManager.get().currentTransformIsDuringCreationRequest();
	}
}