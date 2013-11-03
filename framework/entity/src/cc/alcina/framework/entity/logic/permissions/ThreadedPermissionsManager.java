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

import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestart;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;

/**
 * 
 * @author Nick Reddel
 */
public class ThreadedPermissionsManager extends PermissionsManager {
	@ClearOnAppRestart
	public static GraphProjectionDataFilter INSTANTIATE_IMPL_FILTER;

	@Override
	public PermissionsManager getT() {
		return (ThreadedPermissionsManager) getTTL.get();
	}

	private static ThreadLocal getTTL = new ThreadLocal() {
		protected synchronized Object initialValue() {
			return new ThreadedPermissionsManager();
		}
	};

	public static ThreadedPermissionsManager tpmInstance() {
		return new ThreadedPermissionsManager();
	}

	
	// This should never be necessary, if the code always surrounds user
	// push/pop in try/finally...but...
	public void reset() {
		userStack.clear();
		stateStack.clear();
		setRoot(false);
	}

	@Override
	public void setUser(IUser user) {
		super.setUser(user);
		if (INSTANTIATE_IMPL_FILTER != null) {
			try {
				setInstantiatedUser(new GraphProjection(null,
						INSTANTIATE_IMPL_FILTER).project(user, null));
			} catch (Exception e) {
				if (Registry.impl(JPAImplementation.class)
						.isLazyInitialisationException(e)) {
				} else {
					throw new WrappedRuntimeException(e);
				}
			}
		}
	}

	public static ThreadedPermissionsManager cast() {
		return (ThreadedPermissionsManager) PermissionsManager.get();
	}

	public void popSystemOrCurrentUser() {
		popUser();
	}

	public void pushSystemOrCurrentUserAsRoot() {
		if (isLoggedIn()) {
			pushUser(getUser(), getLoginState(), true);
		} else {
			pushSystemUser();
		}
	}

	@Override
	// TODO - jade - for people with large memberships, this could be cached
	// (memcache) - hardly worthwhile tho
	protected void recursivePopulateGroupMemberships(Set<IGroup> members,
			Set<IGroup> processed) {
		super.recursivePopulateGroupMemberships(members, processed);
	}
}
