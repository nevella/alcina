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
import java.util.function.Predicate;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;

/**
 *
 * @author Nick Reddel
 */
public interface IGroup extends IVersionable, HasDisplayName.Settable {
	public static IGroup byId(long id) {
		Class<? extends IGroup> implementation = PersistentImpl
				.getImplementation(IGroup.class);
		return (IGroup) Domain.find((Class<? extends Entity>) implementation,
				id);
	}

	@Override
	public long getId();

	public Set<? extends IGroup> getMemberGroups();

	public Set<? extends IGroup> getMemberOfGroups();

	public Set<? extends IUser> getMemberUsers();

	public String getName();

	public void setGroupName(String name);

	public void setMemberUsers(Set<? extends IUser> memberUsers);

	default <IG extends Entity & IGroup> void addMemberGroup(IG group) {
		((Entity) this).domain().addToProperty("memberGroups", group);
	}

	default <IU extends Entity & IUser> void addMemberUser(IU user) {
		((Entity) this).domain().addToProperty("memberUsers", user);
	}

	default <IU extends IUser> boolean containsCurrentUser() {
		return getMemberUsers().contains(PermissionsManager.get().getUser());
	}

	default <IG extends IGroup> boolean containsGroup(IG group) {
		return getMemberGroups().contains(group);
	}

	/*
	 * Note - does not descend
	 */
	default <IU extends IUser> boolean containsUser(IU user) {
		if (getName().equals("Developers")) {
			int debug = 3;
		}
		return getMemberUsers().contains(user);
	}

	default <IU extends IUser> boolean
			containsUserOrMemberGroupContainsUser(IU user) {
		boolean forAnyMemberGroups = forAnyMemberGroups(
				group -> group.containsUser(user));
		return forAnyMemberGroups;
	}

	@Override
	default String displayName() {
		return Ax.blankTo(getName(), "(null)");
	}

	default boolean forAnyMemberGroups(Predicate<IGroup> predicate) {
		Set<IGroup> queued = new HashSet<>();
		Stack<IGroup> toTraverse = new Stack<>();
		toTraverse.add(this);
		while (toTraverse.size() > 0) {
			IGroup cursor = toTraverse.pop();
			if (predicate.test(cursor)) {
				return true;
			}
			cursor.getMemberOfGroups().stream().filter(queued::add)
					.forEach(toTraverse::add);
		}
		return false;
	}

	default boolean provideIsMemberOf(IGroup otherGroup) {
		return forAnyMemberGroups(group -> Objects.equals(group, otherGroup));
	}

	@Override
	default void putDisplayName(String name) {
		setGroupName(name);
	}

	default <IU extends Entity & IUser> void removeMemberUser(IU user) {
		((Entity) this).domain().removeFromProperty("memberUsers", user);
	}
}
