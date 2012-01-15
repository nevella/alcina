package cc.alcina.framework.common.client.search;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;

public abstract class TruncatedObjectCriterion<E extends HasId> extends
		SearchCriterion implements HasId {
	private long id;

	private String displayText;

	private transient E value;

	public String getDisplayText() {
		return this.displayText;
	}

	public long getId() {
		return this.id;
	}

	@XmlTransient
	@AlcinaTransient
	public E getValue() {
		return value;
	}

	public boolean equivalentTo(SearchCriterion other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		TruncatedObjectCriterion otherImpl = (TruncatedObjectCriterion) other;
		return otherImpl.getDirection() == getDirection()
				&& getId() == otherImpl.getId();
	}

	public void setDisplayText(String displayText) {
		this.displayText = displayText;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setValue(E value) {
		this.value = value;
		setDisplayText(getDisplayTextFor(value));
		if (value != null) {
			setId(value.getId());
		} else {
			setId(0);
		}
	}

	protected String getDisplayTextFor(E value) {
		return value == null ? null : value.toString();
	}
	@Override
	public String toString() {
		return getDisplayText();
	}
}
