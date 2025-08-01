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
package cc.alcina.framework.common.client.logic.permissions;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;

/**
 *
 * @author Nick Reddel
 */
public interface IUser
		extends IVersionable, HasDisplayName.Settable, AclSubject {
	public static <IU extends IUser> IU current() {
		return (IU) Permissions.get().getUser();
	}

	@Override
	default String displayName() {
		return Ax.blankTo(getUserName(), "(null)");
	}

	public abstract String getEmail();

	public abstract String getFirstName();

	public abstract String getLastName();

	public abstract String getPassword();

	public abstract String getPasswordHash();

	public abstract IGroup getPrimaryGroup();

	public abstract String getSalt();

	public abstract Set<? extends IGroup> getSecondaryGroups();

	public abstract String getUserName();

	public default boolean provideIsAnonymous() {
		return UserlandProvider.get().getAnonymousUser() == this;
	}

	/**
	 * Provide if this user is the current user
	 */
	default boolean provideIsCurrent() {
		return this.equals(Permissions.get().getUser());
	}

	default boolean provideIsMemberOf(IGroup group) {
		Set<IGroup> queued = new HashSet<>();
		Stack<IGroup> toTraverse = new Stack<>();
		toTraverse.addAll(getSecondaryGroups());
		while (toTraverse.size() > 0) {
			IGroup cursor = toTraverse.pop();
			if (Objects.equals(cursor, group)) {
				return true;
			}
			cursor.getMemberOfGroups().stream().filter(queued::add)
					.forEach(toTraverse::add);
		}
		return false;
	}

	@Override
	default void putDisplayName(String name) {
		setUserName(name);
	}

	public void setPassword(String password);

	public void setSalt(String salt);

	public void setUserName(String userName);

	default String toIdNameString() {
		return Ax.format("%s/%s", getId(), getUserName());
	}
}