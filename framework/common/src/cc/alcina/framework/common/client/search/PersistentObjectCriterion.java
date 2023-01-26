package cc.alcina.framework.common.client.search;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

@TypeSerialization(flatSerializable = false)
public class PersistentObjectCriterion extends SearchCriterion {
	private ClassRef classRef;

	private transient String propertyName;

	public String getPropertyName() {
		return this.propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public PersistentObjectCriterion withPropertyName(String propertyName) {
		this.propertyName = propertyName;
		return this;
	}

	public PersistentObjectCriterion() {
	}

	public PersistentObjectCriterion(String displayName) {
		super(displayName);
	}

	@Override
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (classRef == null) {
			return result;
		}
		if (propertyName == null) {
			result.eql = targetPropertyNameWithTable() + ".id = ?";
			result.parameters.add(classRef.getId());
		} else {
			result.eql = Ax.format("(%s.id = ? AND t.propertyName = ?)",
					targetPropertyNameWithTable());
			result.parameters.add(classRef.getId());
			result.parameters.add(propertyName);
		}
		return result;
	}

	@XmlTransient
	@JsonIgnore
	public ClassRef getClassRef() {
		return classRef;
	}

	public void setClassRef(ClassRef classRef) {
		this.classRef = classRef;
	}

	@Override
	public String toString() {
		return classRef == null ? ""
				: "class: "
						+ CommonUtils.simpleClassName(classRef.getRefClass());
	}

	public PersistentObjectCriterion withValue(ClassRef value) {
		setClassRef(value);
		return this;
	}
}
