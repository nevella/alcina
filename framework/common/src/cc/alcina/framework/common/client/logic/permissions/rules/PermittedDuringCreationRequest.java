package cc.alcina.framework.common.client.logic.permissions.rules;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.PermissionsExtensionForRule;

public  class PermittedDuringCreationRequest extends PermissionsExtensionForRule {
	public static final String RULE_NAME = "PermittedDuringCreationRequest";
		public String getRuleName() {
			return RULE_NAME;
		}

		public Boolean isPermitted(Object o, Permissible p) {
			return TransformManager.get().currentTransformIsDuringCreationRequest();
		}
	}