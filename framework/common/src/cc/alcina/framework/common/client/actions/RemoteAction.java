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

import java.io.Serializable;

import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

/**
 * Marker subclass, to be run on the server
 *
 * @author nick@alcina.cc
 */
@WebMethod(customPermission = @Permission(access = AccessLevel.ADMIN))
@ClientInstantiable


public abstract class RemoteAction extends PermissibleAction
		implements Serializable, Task {
	public RemoteAction() {
	}

	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.ADMIN;
	}
}
