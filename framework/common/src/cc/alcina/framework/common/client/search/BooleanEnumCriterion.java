package cc.alcina.framework.common.client.search;

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
	public String toString() {
		return booleanEnum == null ? ""
				: (booleanEnum == BooleanEnum.TRUE) ? "yes" : "no";
	}

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		BooleanEnum value = getValue();
		if (value != null
				&& !CommonUtils.isNullOrEmpty(getTargetPropertyName())) {
			result.eql = targetPropertyNameWithTable() + " = ? ";
			result.parameters.add(valueAsString() ? value.toString() : Boolean
					.valueOf(value.toString()));
		}
		return result;
	}

	@Override
	public BooleanEnum getValue() {
		return getBooleanEnum();
	}

	@Override
	public void setValue(BooleanEnum value) {
		setBooleanEnum(value);
	}

	public BooleanEnum getBooleanEnum() {
		return booleanEnum;
	}

	public boolean toBoolean() {
		return booleanEnum == BooleanEnum.TRUE;
	}

	public void setBooleanEnum(BooleanEnum booleanEnum) {
		BooleanEnum old_booleanEnum = this.booleanEnum;
		this.booleanEnum = booleanEnum;
		propertyChangeSupport().firePropertyChange("booleanEnum",
				old_booleanEnum, booleanEnum);
	}
}