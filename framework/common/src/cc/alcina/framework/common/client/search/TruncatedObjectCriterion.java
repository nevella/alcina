package cc.alcina.framework.common.client.search;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public abstract class TruncatedObjectCriterion<E extends HasId>
		extends SearchCriterion implements HasId {
	static final transient long serialVersionUID = 1;

	private String displayText;

	private transient E value;

	protected E forClientTrimmed;

	private long id;

	public TruncatedObjectCriterion() {
		setOperator(StandardSearchOperator.EQUALS);
	}

	public void depopulateValue() {
		forClientTrimmed = null;
		value = null;
	}

	public E ensurePlaceholderObject() {
		if (value == null && id != 0) {
			value = Reflections.newInstance(getObjectClass());
			value.setId(id);
		}
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof SearchCriterion
				&& equivalentTo((SearchCriterion) obj);
	}

	@Override
	public boolean equivalentTo(SearchCriterion other) {
		if (other instanceof TruncatedObjectCriterion) {
			return other.getClass() == getClass()
					&& ((TruncatedObjectCriterion) other).getId() == getId();
		}
		return super.equivalentTo(other);
	}

	public String getDisplayText() {
		return this.displayText;
	}

	@Override
	public long getId() {
		return this.id;
	}

	public abstract Class<E> getObjectClass();

	@XmlTransient
	@JsonIgnore
	@AlcinaTransient
	public E getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode() ^ (int) getId();
	}

	public void populateValue() {
	}

	public String provideTypeDisplayName() {
		return CommonUtils.titleCase(
				CommonUtils.deInfix(getObjectClass().getSimpleName()));
	}

	public void setDisplayText(String displayText) {
		this.displayText = displayText;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setValue(E value) {
		setDisplayText(getDisplayTextFor(value));
		if (value != null) {
			setId(value.getId());
		} else {
			setId(0);
		}
		E old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	@Override
	public String toString() {
		return getDisplayText();
	}

	public <T extends TruncatedObjectCriterion<E>> T withObject(E withValue) {
		setValue(withValue);
		return (T) this;
	}

	protected String getDisplayTextFor(E value) {
		return value == null ? null : value.toString();
	}
}
