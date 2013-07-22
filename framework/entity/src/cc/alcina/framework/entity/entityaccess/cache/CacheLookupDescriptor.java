package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.PropertyPathAccesor;

public class CacheLookupDescriptor {
	public Class clazz;

	public String propertyPath;

	public boolean handles(Class clazz2, String propertyPath) {
		return clazz2 == clazz && propertyPath.equals(this.propertyPath);
	}

	public CacheLookupDescriptor(Class clazz, String propertyPath) {
		this.clazz = clazz;
		this.propertyPath = propertyPath;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lookup descriptor - %s :: %s", clazz,
				propertyPath);
	}
}
