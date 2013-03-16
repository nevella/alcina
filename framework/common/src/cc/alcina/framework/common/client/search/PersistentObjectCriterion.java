package cc.alcina.framework.common.client.search;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.util.CommonUtils;

public class PersistentObjectCriterion extends SearchCriterion {
	public PersistentObjectCriterion() {
	}

	public PersistentObjectCriterion(String displayName) {
		super(displayName);
	}

	private ClassRef classRef;

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (classRef == null) {
			return result;
		}
		result.eql = targetPropertyNameWithTable() + ".id = ?";
		result.parameters.add(classRef.getId());
		return result;
	}
	public boolean equivalentTo(SearchCriterion other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		PersistentObjectCriterion otherImpl = (PersistentObjectCriterion) other;
		return otherImpl.getDirection() == getDirection()
				&& CommonUtils.equalsWithNullEquality(getClassRef(), otherImpl
						.getClassRef());
	}

	@Override
	public String toString() {
		return classRef == null ? "" : "class: "
				+ CommonUtils.simpleClassName(classRef.getRefClass());
	}

	public void setClassRef(ClassRef classRef) {
		this.classRef = classRef;
	}

	@XmlTransient
	public ClassRef getClassRef() {
		return classRef;
	}
	@Override
	public PersistentObjectCriterion clone() throws CloneNotSupportedException {
		PersistentObjectCriterion copy = new PersistentObjectCriterion();
		copy.copyProperties(this);
		copy.classRef=classRef;
		return copy;
	}
}
