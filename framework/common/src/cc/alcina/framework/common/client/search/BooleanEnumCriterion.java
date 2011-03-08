package cc.alcina.framework.common.client.search;

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

	public void setBooleanEnum(BooleanEnum booleanEnum) {
		BooleanEnum old_booleanEnum = this.booleanEnum;
		this.booleanEnum = booleanEnum;
		propertyChangeSupport.firePropertyChange("booleanEnum",
				old_booleanEnum, booleanEnum);
	}

	public BooleanEnum getBooleanEnum() {
		return booleanEnum;
	}

	@Override
	public BooleanEnum getValue() {
		return getBooleanEnum();
	}

	@Override
	public void setValue(BooleanEnum value) {
		setBooleanEnum(value);
	}
}