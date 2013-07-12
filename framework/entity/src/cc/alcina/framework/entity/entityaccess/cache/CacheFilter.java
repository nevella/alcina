package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilter;

public class CacheFilter {
	public String propertyName;
	public Object propertyValue;
	public CollectionFilter collectionFilter;
	public CacheFilter(String propertyName, Object propertyValue) {
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}
	public CacheFilter(CollectionFilter collectionFilter) {
		this.collectionFilter = collectionFilter;
	}
	public static List<CacheFilter> fromKvs(Object... objects){
		List<CacheFilter> result=new ArrayList<CacheFilter>();
		for (int i = 0; i < objects.length; i+=2) {
			result.add(new CacheFilter((String) objects[i],objects[i+1]));
		}
		return result;
	}
}
