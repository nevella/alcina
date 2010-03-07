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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.entity.datatransform.EntityLayerLocator;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.util.GraphCloner;
import cc.alcina.framework.entity.util.GraphCloner.CloneFilter;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class ThreadedPermissionsManager extends PermissionsManager {
	public static CloneFilter INSTANTIATE_IMPL_FILTER;

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

	public IUser pushSystemUser() {
		CommonPersistenceLocal up = EntityLayerLocator.get()
				.commonPersistenceProvider().getCommonPersistence();
		IUser systemUser = up.getSystemUser(true);
		pushUser(systemUser, LoginState.LOGGED_IN);
		root = true;
		return systemUser;
	}

	public IUser popSystemUser() {
		root = false;
		return popUser();
	}

	protected boolean isRoot() {
		return root;
	}

	private boolean root;

	@Override
	public void setUser(IUser user) {
		super.setUser(user);
		if (INSTANTIATE_IMPL_FILTER != null) {
			try {
				setInstantiatedUser(new GraphCloner(null,
						INSTANTIATE_IMPL_FILTER).clone(user, null));
			} catch (Exception e) {
				if (EntityLayerLocator.get().jpaImplementation()
						.isLazyInitialisationException(e)) {
				} else {
					throw new WrappedRuntimeException(e);
				}
			}
		}
	}
}
