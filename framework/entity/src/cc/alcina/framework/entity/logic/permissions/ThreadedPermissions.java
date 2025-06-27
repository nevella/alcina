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
package cc.alcina.framework.entity.logic.permissions;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.entity.logic.ServerClientInstance;

/**
 * @author Nick Reddel
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class ThreadedPermissions extends Permissions {
	private static ThreadLocal threadLocalInstance = new ThreadLocal() {
		@Override
		protected synchronized Object initialValue() {
			return new ThreadedPermissions();
		}
	};

	public static ThreadedPermissions cast0() {
		return (ThreadedPermissions) Permissions.get();
	}

	public static boolean is() {
		return get() instanceof ThreadedPermissions;
	}

	public static ThreadedPermissions tpmInstance() {
		return new ThreadedPermissions();
	}

	@Override
	protected Permissions getPerThreadInstance() {
		return (ThreadedPermissions) threadLocalInstance.get();
	}

	public <IU extends IUser> IU provideNonSystemUserInStackOrThrow() {
		return provideNonSystemUserInStackOrThrow(false);
	}

	public <IU extends IUser> IU
			provideNonSystemUserInStackOrThrow(boolean throwIfNotFound) {
		int idx = stateStack.size() - 1;
		while (idx >= 0) {
			Boolean isRoot = stateStack.get(idx).root;
			if (!isRoot) {
				return (IU) stateStack.get(idx).user;
			}
			idx--;
		}
		if (throwIfNotFound) {
			throw new IllegalStateException("No non-root user in stack");
		} else {
			return null;
		}
	}

	@Override
	protected void removePerThreadContext0() {
		threadLocalInstance.remove();
	}

	@Registration(GetSystemUserClientInstance.class)
	public static class GetSystemUserClientInstanceImpl
			implements GetSystemUserClientInstance {
		@Override
		public ClientInstance getClientInstance() {
			ClientInstance serverAsClientInstance = ServerClientInstance.get();
			/*
			 * Note that this is null until bootstrap/server client instance
			 * creation
			 */
			return serverAsClientInstance;
		}
	}
}
