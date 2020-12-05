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

import java.util.concurrent.Callable;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.ThrowingRunnable;

/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class ThreadedPermissionsManager extends PermissionsManager {
	private static ThreadLocal getTTL = new ThreadLocal() {
		@Override
		protected synchronized Object initialValue() {
			return new ThreadedPermissionsManager();
		}
	};

	public static ThreadedPermissionsManager cast() {
		return (ThreadedPermissionsManager) PermissionsManager.get();
	}

	public static void clearThreadLocal() {
		getTTL.remove();
	}

	public static boolean is() {
		return get() instanceof ThreadedPermissionsManager;
	}

	public static ThreadedPermissionsManager tpmInstance() {
		return new ThreadedPermissionsManager();
	}

	public <T> T callWithPushedSystemUserIfNeeded(Callable<T> callable)
			throws Exception {
		if (isRoot()) {
			return callable.call();
		} else {
			try {
				pushSystemUser();
				return callable.call();
			} finally {
				popSystemUser();
			}
		}
	}

	public <T> T callWithPushedSystemUserIfNeededNoThrow(Callable<T> callable) {
		try {
			if (isRoot()) {
				return callable.call();
			} else {
				try {
					pushSystemUser();
					return callable.call();
				} finally {
					popSystemUser();
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public ClientInstance getClientInstance() {
		return ThreadedPmClientInstanceResolver.get().getClientInstance();
	}

	@Override
	public Long getClientInstanceId() {
		return ThreadedPmClientInstanceResolver.get().getClientInstanceId();
	}

	@Override
	public PermissionsManager getT() {
		return (ThreadedPermissionsManager) getTTL.get();
	}

	public boolean isSystemUser() {
		return getUserName().equals(PermissionsManager.SYSTEM_USER_NAME);
	}

	public void popSystemOrCurrentUser() {
		popUser();
	}

	public <IU extends IUser> IU provideNonSystemUserInStackOrThrow() {
		return provideNonSystemUserInStackOrThrow(false);
	}

	public <IU extends IUser> IU
			provideNonSystemUserInStackOrThrow(boolean throwIfNotFound) {
		int idx = stateStack.size() - 1;
		while (idx >= 0) {
			Boolean isRoot = stateStack.get(idx).asRoot;
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

	public void pushSystemOrCurrentUserAsRoot() {
		if (isLoggedIn()) {
			pushUser(getUser(), getLoginState(), true);
		} else {
			pushSystemUser();
		}
	}

	// This should never be necessary, if the code always surrounds user
	// push/pop in try/finally...but...
	public void reset() {
		stateStack.clear();
		setRoot(false);
	}

	public void
			runThrowingWithPushedSystemUserIfNeeded(ThrowingRunnable runnable) {
		try {
			if (isRoot()) {
				runnable.run();
			} else {
				try {
					pushSystemUser();
					runnable.run();
				} finally {
					popSystemUser();
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void runWithPushedSystemUserIfNeeded(Runnable runnable) {
		try {
			if (isRoot()) {
				runnable.run();
			} else {
				try {
					pushSystemUser();
					runnable.run();
				} finally {
					popSystemUser();
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}