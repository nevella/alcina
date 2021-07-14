package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.flat.PropertySerialization;
import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class BooleanEnumCriterion extends EnumCriterion<BooleanEnum> {
	static final transient long serialVersionUID = -1L;

	private BooleanEnum booleanEnum;

	public BooleanEnumCriterion() {
		super();
	}

	public BooleanEnumCriterion(String criteriaDisplayName, boolean withNull) {
		super(criteriaDisplayName, withNull);
	}

	@Override
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		BooleanEnum value = getValue();
		if (value != null
				&& !CommonUtils.isNullOrEmpty(getTargetPropertyName())) {
			result.eql = targetPropertyNameWithTable() + " = ? ";
			result.parameters.add(valueAsString() ? value.toString()
					: Boolean.valueOf(value.toString()));
		}
		return result;
	}

	@AlcinaTransient
	public BooleanEnum getBooleanEnum() {
		return booleanEnum;
	}

	@Override
	@PropertySerialization(defaultProperty = true)
	public BooleanEnum getValue() {
		return getBooleanEnum();
	}

	public void setBooleanEnum(BooleanEnum booleanEnum) {
		BooleanEnum old_booleanEnum = this.booleanEnum;
		this.booleanEnum = booleanEnum;
		propertyChangeSupport().firePropertyChange("booleanEnum",
				old_booleanEnum, booleanEnum);
		propertyChangeSupport().firePropertyChange("value", old_booleanEnum,
				booleanEnum);
	}

	@Override
	public void setValue(BooleanEnum value) {
		setBooleanEnum(value);
	}

	public Boolean toBoolean() {
		return BooleanEnum.toBoolean(booleanEnum);
	}

	public boolean toBooleanPrimitive() {
		return booleanEnum == BooleanEnum.TRUE;
	}

	@Override
	public String toString() {
		return booleanEnum == null ? ""
				: (booleanEnum == BooleanEnum.TRUE) ? "yes" : "no";
	}
}
