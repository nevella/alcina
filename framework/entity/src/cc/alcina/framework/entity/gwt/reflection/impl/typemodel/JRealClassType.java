package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

public class JRealClassType extends JClassType
		implements com.google.gwt.core.ext.typeinfo.JRealClassType {
	public JRealClassType(TypeOracle typeOracle, Class clazz) {
		super(typeOracle, clazz);
	}

	@Override
	public JClassType getErasedType() {
		return this;
	}

	@Override
	public long getLastModifiedTime() {
		throw new UnsupportedOperationException();
	}
}
