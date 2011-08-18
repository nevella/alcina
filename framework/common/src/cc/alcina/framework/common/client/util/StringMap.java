/**
 * 
 */
package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class StringMap extends LinkedHashMap<String, String> {
	public static final StringMap EMPTY_PROPS = new StringMap();
	public static StringMap property(String key,String value){
		StringMap map = new StringMap();
		map.put(key,value);
		return map;
	}
	public String toPropertyString(){
		StringBuilder sb=new StringBuilder();
		for (Map.Entry<String, String> entry : entrySet()) {
			if(sb.length()!=0){
				sb.append("\n");
			}
			sb.append(entry.getKey());
			sb.append('=');
			sb.append(entry.getValue().replace("=", "\\=").replace("\n", "\\n"));
		}
		return sb.toString();
	}
}