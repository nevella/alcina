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
package cc.alcina.framework.common.client.entity;

import java.util.Date;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;

/**
 * Only used in entity-layer code
 * 
 * @author Nick Reddel
 */
public class VersioningEntityListener {
	public static ThreadLocal<Boolean> disabled = new ThreadLocal() {
		@Override
		protected synchronized Boolean initialValue() {
			return false;
		}
	};

	@PrePersist
	@PreUpdate
	public void setVersioningInfo(Object obj) {
		if (obj instanceof IVersionable && !disabled.get()) {
			IVersionable iv = (IVersionable) obj;
			Date now = new Date();
			if (iv.getLastModificationDate() == null
					&& iv.getCreationDate() == null) {
				iv.setCreationDate(now);
			}
			iv.setLastModificationDate(now);
		}
		if (obj instanceof IUser) {
		}
	}
}
