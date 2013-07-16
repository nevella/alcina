package cc.alcina.framework.entity.entityaccess.cache;

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
}
