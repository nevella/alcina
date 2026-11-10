package cc.alcina.framework.common.client.search;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public abstract class EntityCriterion<E extends Entity> extends SearchCriterion
		implements HasId {
	static final transient long serialVersionUID = 1;

	private String displayText;

	private transient E value;

	protected E forClientTrimmed;

	private long id;

	public EntityCriterion() {
		setOperator(StandardSearchOperator.EQUALS);
	}

	public void depopulateValue() {
		forClientTrimmed = null;
		value = null;
	}

	@Override
	public boolean emptyCriterion() {
		return getId() == 0;
	}

	public E ensurePlaceholderObject() {
		if (value == null && id != 0) {
			value = Reflections.newInstance(getObjectClass());
			value.setId(id);
		}
		return value;
	}

	@Override
	public boolean equivalentTo(SearchCriterion other) {
		if (other instanceof EntityCriterion) {
			return other.getClass() == getClass()
					&& ((EntityCriterion) other).getId() == getId();
		}
		return super.equivalentTo(other);
	}

	@PropertySerialization(fromClient = false)
	public String getDisplayText() {
		return this.displayText;
	}

	@Override
	@PropertySerialization(defaultProperty = true)
	public long getId() {
		return this.id;
	}

	public Class<E> getObjectClass() {
		return Reflections.at(this).firstGenericBound();
	}

	@XmlTransient
	@JsonIgnore
	@AlcinaTransient
	public E getValue() {
		if (value == null && id != 0) {
			value = (E) Domain.find((Class<? extends Entity>) getObjectClass(),
					id);
			if (value == null) {
				if (GWT.isClient()) {
					value = Reflections.newInstance(getObjectClass());
					value.setId(id);
				}
			}
		}
		return value;
	}

	public void populateValue() {
	}

	protected String provideDisplayTextFor(E value) {
		return value == null ? null : value.toString();
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
		if (Ax.isBlank(getDisplayText())) {
			setDisplayText(provideDisplayTextFor(value));
		}
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
		if (getId() == 0) {
			return CommonUtils
					.deInfix(NestedName.get(this).replace("Criterion", ""));
		}
		return Ax.blankTo(getDisplayText(),
				() -> Ax.format("(id:%s)", getId()));
	}

	public <T extends EntityCriterion<E>> T withObject(E withValue) {
		setValue(withValue);
		return (T) this;
	}

	@Override
	public boolean requiresDisplayText() {
		return getId() == 0 ? false : Ax.notBlank(getDisplayName());
	}
}
