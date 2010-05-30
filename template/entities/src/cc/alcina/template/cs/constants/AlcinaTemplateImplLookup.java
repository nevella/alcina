package cc.alcina.template.cs.constants;

import cc.alcina.framework.common.client.logic.domaintransform.spi.ImplementationLookup;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;


public class AlcinaTemplateImplLookup implements ImplementationLookup {
	public Class getImplementation(Class interfaceClass) {
		if (interfaceClass == IUser.class) {
			return AlcinaTemplateUser.class;
		}
		if (interfaceClass == IGroup.class) {
			return AlcinaTemplateGroup.class;
		}
		return null;
	}
}
