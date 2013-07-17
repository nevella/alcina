package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.util.CommonUtils;

public class CacheLookupDescriptor {
	public Class clazz;
	public String fieldName1;
	public boolean handles(Class clazz2, String propertyName) {
		return clazz2==clazz&&propertyName.equals(fieldName1);
	}
	public CacheLookupDescriptor(Class clazz, String fieldName1) {
		this.clazz = clazz;
		this.fieldName1 = fieldName1;
	}
	@Override
	public String toString() {
		return CommonUtils.formatJ("Lookup descriptor - %s :: %s",clazz,fieldName1);
	}
}
