package cc.alcina.framework.common.client.search;

import javax.xml.bind.annotation.XmlTransient;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.util.CommonUtils;

public class PersistentObjectCriterion extends SearchCriterion {

    static final transient long serialVersionUID = -1L;

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

    @Override
    public String toString() {
        return classRef == null ? "" : "class: " + CommonUtils.simpleClassName(classRef.getRefClass());
    }

    public void setClassRef(ClassRef classRef) {
        this.classRef = classRef;
    }

    @XmlTransient
    public ClassRef getClassRef() {
        return classRef;
    }
}
