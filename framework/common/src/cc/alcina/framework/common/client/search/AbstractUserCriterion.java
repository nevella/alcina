package cc.alcina.framework.common.client.search;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.logic.domain.HasValue;

public abstract class AbstractUserCriterion extends SearchCriterion
		implements HasValue<Long> {
	public AbstractUserCriterion() {
	}

	public AbstractUserCriterion(String displayName) {
		super(displayName);
	}

	public abstract Long getUserId();

	@XmlTransient
	@JsonIgnore
	public Long getValue() {
		return getUserId();
	}

	public abstract void setUserId(Long value);

	public void setValue(Long value) {
		setUserId(value);
	}
}
