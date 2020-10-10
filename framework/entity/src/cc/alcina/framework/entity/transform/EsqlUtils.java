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
package cc.alcina.framework.entity.transform;

/**
 *
 * @author Nick Reddel
 */
public class EsqlUtils {
	public static String idArrToIn(Long[] ids) {
		StringBuffer result = new StringBuffer("(-1");
		for (Long long1 : ids) {
			result.append(", ");
			result.append(long1.toString());
		}
		result.append(") ");
		return result.toString();
	}
}
