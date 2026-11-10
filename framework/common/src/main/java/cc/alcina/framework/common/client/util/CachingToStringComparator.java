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
package cc.alcina.framework.common.client.util;

import java.util.Comparator;
import java.util.Map;

/**
 *
 * @author Nick Reddel
 */
public class CachingToStringComparator implements Comparator {
	Map<Object, String> objectToString = AlcinaCollections.newHashMap();

	@Override
	public int compare(Object o1, Object o2) {
		String s1 = objectToString.computeIfAbsent(o1,
				o -> o == null ? null : o.toString());
		String s2 = objectToString.computeIfAbsent(o2,
				o -> o == null ? null : o.toString());
		if (s1==null) {
			return o2 == null ? -1 : 0;
		}
		if (s2 == null) {
			return 1;
		}
		return s1.compareTo(s2);
	}
}