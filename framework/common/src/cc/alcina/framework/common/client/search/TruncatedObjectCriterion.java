package cc.alcina.framework.common.client.search;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.flat.PropertySerialization;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public abstract class TruncatedObjectCriterion<E extends HasId>
		extends SearchCriterion implements HasId {
	private static Map<EntityLocator, String> clientDisplayTexts;

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
	public boolean equivalentTo(SearchCriterion other) {
		if (other instanceof TruncatedObjectCriterion) {
			return other.getClass() == getClass()
					&& ((TruncatedObjectCriterion) other).getId() == getId();
		}
		return super.equivalentTo(other);
	}

	@PropertySerialization(fromClient = false)
	public String getDisplayText() {
		if (GWT.isClient()) {
			Map<EntityLocator, String> clientDisplayTexts = ensureClientDisplayTexts();
			if (id != 0) {
				try {
					EntityLocator locator = new EntityLocator(
							(Class) getObjectClass(), id, 0L);
					if (displayText != null) {
						clientDisplayTexts.put(locator, displayText);
					} else {
						this.displayText = clientDisplayTexts.get(locator);
					}
				} catch (Exception e) {
					// FIXME - meta - add isassignable (from Entity) check;
					// should only fail
					// if isAssignable fails
					e.printStackTrace();
				}
			}
		}
		return this.displayText;
	}

	@Override
	@PropertySerialization(defaultProperty = true)
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
		setDisplayText(provideDisplayTextFor(value));
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

	private Map<EntityLocator, String> ensureClientDisplayTexts() {
		if (clientDisplayTexts == null) {
			clientDisplayTexts = new LinkedHashMap<>();
		}
		return clientDisplayTexts;
	}

	protected String provideDisplayTextFor(E value) {
		return value == null ? null : value.toString();
	}
}
