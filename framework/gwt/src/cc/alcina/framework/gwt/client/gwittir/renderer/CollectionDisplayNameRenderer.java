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
package cc.alcina.framework.gwt.client.gwittir.renderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import cc.alcina.framework.common.client.logic.reflection.ClientReflector;

/**
 *
 * @author Nick Reddel
 */
public class CollectionDisplayNameRenderer extends FlexibleToStringRenderer {
	public static final CollectionDisplayNameRenderer INSTANCE = new CollectionDisplayNameRenderer();

	public String render(Object o) {
		if (o == null || !(o instanceof Collection)) {
			return "(Undefined)";
		}
		String result = "";
		ArrayList l = new ArrayList((Collection) o);
		Collections.sort(l);
		for (Object object : l) {
			if (result.length() != 0) {
				result += ", ";
			}
			result += ClientReflector.get().displayNameForObject(object);
		}
		return result;
	}
}