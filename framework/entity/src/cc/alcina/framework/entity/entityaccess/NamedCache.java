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

package cc.alcina.framework.entity.entityaccess;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */

 public class NamedCache {
	public static int cacheSize = 100;
	public static final String ALL_CACHE="A_CACHE";
	private static Map<String, Map> caches = new LinkedHashMap<String, Map>();

	public static <T> T get(String mapName, Object key) {
		checkMap(mapName);
		return (T) caches.get(mapName).get(key);
	}

	public static void put(String mapName, Object key, Object value) {
		checkMap(mapName);
		caches.get(mapName).put(key, value);
	}

	public static boolean containsKey(String mapName, Object key) {
		return get(mapName, key) != null;
	}
	public static boolean isEmpty(String mapName) {
		checkMap(mapName);
		return caches.get(mapName).isEmpty();
		
	}
	private static synchronized void checkMap(String mapName){
		if (!caches.containsKey(mapName)) {
			caches.put(mapName, new LRUMap(cacheSize));
		}
	}

	public static void invalidate(String mapName) {
		checkMap(mapName);
		caches.get(mapName).clear();
	}
	public static void invalidate(String mapName,Object key) {
		checkMap(mapName);
		caches.get(mapName).remove(key);
	}
}
