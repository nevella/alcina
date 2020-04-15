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

import java.util.Set;

/**
 * 
 * @author Nick Reddel
 */
public interface IGroup extends IVersionable {
	@Override
	public long getId();

	public Set<? extends IGroup> getMemberGroups();

	public Set<? extends IGroup> getMemberOfGroups();

	public Set<? extends IUser> getMemberUsers();

	public String getName();

	public Set<IGroup> listMemberGroups();

	public Set<IUser> listMemberUsers();

	public void setGroupName(String name);

	public void setMemberUsers(Set<? extends IUser> memberUsers);

	default <IG extends IGroup> boolean containsGroup(IG group) {
		return getMemberGroups().contains(group);
	}

	default <IU extends IUser> boolean containsUser(IU user) {
		return getMemberUsers().contains(user);
	}
}
