package cc.alcina.framework.common.client.search;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.util.CommonUtils;

public class PersistentObjectCriterion extends SearchCriterion {
	static final transient long serialVersionUID = -1L;

	private ClassRef classRef;

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
		result.eql = targetPropertyNameWithTable() + ".id = ?";
		result.parameters.add(classRef.getId());
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

	public PersistentObjectCriterion withValue(ClassRef value) {
		setClassRef(value);
		return this;
	}

	@Override
	public String toString() {
		return classRef == null ? ""
				: "class: "
						+ CommonUtils.simpleClassName(classRef.getRefClass());
	}
}
