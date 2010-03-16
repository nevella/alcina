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

/**
 *
 * @author Nick Reddel
 */

 public class ToStringComparator implements Comparator {
	public static final ToStringComparator INSTANCE = new ToStringComparator();

	public int compare(Object o1, Object o2) {
		if (o1 == null || o1.toString() == null) {
			return o2 == null ? -1 : 0;
		}
		if (o2 == null || o2.toString() == null) {
			return 1;
		}
		return o1.toString().compareTo(o2.toString());
	}
}