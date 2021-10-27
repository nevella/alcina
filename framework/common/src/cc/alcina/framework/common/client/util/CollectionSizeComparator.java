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

import java.util.Collection;
import java.util.Comparator;

/**
 *
 * @author Nick Reddel
 */
public class CollectionSizeComparator implements Comparator<Collection> {
	public static final CollectionSizeComparator INSTANCE = new CollectionSizeComparator();

	public int compare(Collection o1, Collection o2) {
		if (o1 == null) {
			return o2 == null ? 0 : -1;
		}
		return o2 == null ? 1 : Integer.valueOf(o1.size()).compareTo(o2.size());
	}
}
