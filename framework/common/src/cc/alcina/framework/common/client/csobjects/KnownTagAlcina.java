package cc.alcina.framework.common.client.csobjects;

public enum KnownTagAlcina implements KnownTag {
	Root(null),
	/*
	 * 
	 */
	Status(Root), Status_Ok(Status), Status_Info(Status), Status_Warn(Status),
	Status_Error(Status),
	/*
	 * 
	 */
	Area(Root), Area_Code(Area), Area_Devops(Area), Area_None(Area);
	private KnownTag parent;

	private KnownTagAlcina(KnownTag parent) {
		this.parent = parent;
	}

	@Override
	public KnownTag parent() {
		return parent;
	}

	public boolean isOrAncestorIs(KnownTagAlcina test) {
		if (this == test) {
			return true;
		}
		if (parent == null) {
			return false;
		}
		return ((KnownTagAlcina) parent).isOrAncestorIs(test);
	}
}
