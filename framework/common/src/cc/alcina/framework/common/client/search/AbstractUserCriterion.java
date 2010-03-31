package cc.alcina.framework.common.client.search;

public abstract class AbstractUserCriterion extends SearchCriterion {
	public AbstractUserCriterion() {
	}

	public AbstractUserCriterion(String displayName) {
		super(displayName);
	}

	public abstract Long getUserId();
}
