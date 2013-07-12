package cc.alcina.framework.entity.entityaccess.cache;

public class CacheLookupDescriptor {
	public Class clazz;
	public String fieldName1;
	public String fieldName2;
	public boolean singleField() {
		return fieldName2==null;
	}
	public boolean handles(Class clazz2, String propertyName) {
		return clazz2==clazz&&propertyName.equals(fieldName1)&&singleField();
	}
	public CacheLookupDescriptor(Class clazz, String fieldName1) {
		this.clazz = clazz;
		this.fieldName1 = fieldName1;
	}
}
