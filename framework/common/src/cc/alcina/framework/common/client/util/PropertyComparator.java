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

import cc.alcina.framework.common.client.reflection.Reflections;

/**
 *
 * @author Nick Reddel
 */
public class PropertyComparator implements Comparator {
	private final String propertyName;

	public PropertyComparator(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public int compare(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return 0;
		}
		if (o1 == null) {
			return -1;
		}
		if (o2 == null) {
			return 1;
		}
		try {
			Object pv1 = Reflections.at(o1).property(propertyName).get(o1);
			Object pv2 = Reflections.at(o2).property(propertyName).get(o2);
			return CommonUtils.compareWithNullMinusOne((Comparable) pv1,
					(Comparable) pv2);
		} catch (Exception e) {
			return 0;
		}
	}
}
