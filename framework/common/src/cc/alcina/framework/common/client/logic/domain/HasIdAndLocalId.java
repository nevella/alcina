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

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public interface HasIdAndLocalId extends HasId {
	public long getLocalId();

	public void setLocalId(long localId);

	public static class HiliComparator implements Comparator<HasIdAndLocalId> {
		public static final HiliComparator INSTANCE = new HiliComparator();

		public int compare(HasIdAndLocalId o1, HasIdAndLocalId o2) {
			if (o1 == null) {
				return o2 == null ? 0 : -1;
			}
			return o2 == null ? 1 : new Long(o1.getId()).compareTo(o2.getId());
		}
	}

	public static class HiliHelper {
		public static boolean equals(HasIdAndLocalId o1, Object o2) {
			if (o1.getId() == 0 && o1.getLocalId() == 0) {
				return o1 == o2;
			}
			if (o2 instanceof HasIdAndLocalId) {
				HasIdAndLocalId hili = (HasIdAndLocalId) o2;
				return (hili.getId() == o1.getId()
						&& hili.getLocalId() == o1.getLocalId() && hili
						.getClass().equals(o1.getClass()));
			}
			return false;
		}

		public static Set<Long> toIdSet(
				Collection<? extends HasIdAndLocalId> hilis) {
			Set<Long> result = new LinkedHashSet<Long>();
			for (HasIdAndLocalId hili : hilis) {
				result.add(hili.getId());
			}
			return result;
		}

		public static String asDomainPoint(HasId hi) {
			if (hi instanceof HasIdAndLocalId) {
				HasIdAndLocalId hili = (HasIdAndLocalId) hi;
				return CommonUtils.format("Hili: %1 : %2 / %3", CommonUtils
						.simpleClassName(hili.getClass()), hili.getId(), hili
						.getLocalId());
			}
			return CommonUtils.format("HasId: %1 : %2 ", CommonUtils
					.simpleClassName(hi.getClass()), hi.getId());
		}
	}
}
