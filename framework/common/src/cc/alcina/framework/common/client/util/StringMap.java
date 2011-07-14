/**
 * 
 */
package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;

public class StringMap extends LinkedHashMap<String, String> {
	public static final StringMap EMPTY_PROPS = new StringMap();
	public static StringMap property(String key,String value){
		StringMap map = new StringMap();
		map.put(key,value);
		return map;
	}
}