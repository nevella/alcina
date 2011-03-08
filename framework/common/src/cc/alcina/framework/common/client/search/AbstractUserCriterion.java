package cc.alcina.framework.common.client.search;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.domain.HasValue;

public abstract class AbstractUserCriterion extends SearchCriterion implements
		HasValue<Long> {
	static final transient long serialVersionUID = -1L;
	public AbstractUserCriterion() {
	}

	public AbstractUserCriterion(String displayName) {
		super(displayName);
	}

	public abstract Long getUserId();

	public abstract void setUserId(Long value);

	@XmlTransient
	public Long getValue() {
		return getUserId();
	}

	public void setValue(Long value) {
		setUserId(value);
	}
}
