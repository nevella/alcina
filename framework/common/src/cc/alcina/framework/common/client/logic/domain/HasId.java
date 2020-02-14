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
package cc.alcina.framework.common.client.logic.domain;

import java.io.Serializable;
import java.util.Comparator;

import cc.alcina.framework.common.client.util.Ax;

/**
 *
 * @author Nick Reddel
 */
public interface HasId extends Serializable {
	public static final Comparator<HasId> HAS_ID_COMPARATOR = new Comparator<HasId>() {
		@Override
		public int compare(HasId o1, HasId o2) {
			return new Long(o1.getId()).compareTo(o2.getId());
		}
	};

	public long getId();

	public void setId(long id);

	default String toStringId() {
		return Ax.format("%s/%s", getClass().getSimpleName(), getId());
	}
}
