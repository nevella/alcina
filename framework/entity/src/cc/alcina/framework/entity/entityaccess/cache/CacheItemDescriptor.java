package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.List;

public class CacheItemDescriptor {
	public Class clazz;
	public List<CacheLookupInfo> lookups=new ArrayList<CacheLookupInfo>();
	public boolean lazy=false;
	public CacheItemDescriptor(Class clazz) {
		this.clazz = clazz;
	}
	public CacheItemDescriptor addLookup(CacheLookupInfo lookup){
		lookups.add(lookup);
		return this;
	}
}
