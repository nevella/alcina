/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;

@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */
public class DomainTransformEvent
        implements Serializable, Comparable<DomainTransformEvent>, Cloneable {
    public static transient final String CONTEXT_IGNORE_UNHANDLED_DOMAIN_CLASSES = DomainTransformEvent.class
            .getName() + ".CONTEXT_IGNORE_UNHANDLED_DOMAIN_CLASSES";

    public static transient final Comparator<DomainTransformEvent> UTC_DATE_COMPARATOR = new Comparator<DomainTransformEvent>() {
        @Override
        public int compare(DomainTransformEvent o1, DomainTransformEvent o2) {
            return CommonUtils.compareDates(o1.getUtcDate(), o2.getUtcDate());
        }
    };

    private String propertyName;

    private transient Object newValue;

    private transient HasIdAndLocalId source;

    private transient Class objectClass;

    private String objectClassName;

    private String valueClassName;

    private ClassRef objectClassRef;

    private ClassRef valueClassRef;

    private long eventId;

    private long objectId;

    private long objectLocalId;

    private long generatedServerId;

    private Integer objectVersionNumber;

    private Integer valueVersionNumber;

    private transient Class valueClass;

    private long valueId;

    private long valueLocalId;

    private String newStringValue;

    private TransformType transformType;

    private CommitType commitType = CommitType.TO_LOCAL_BEAN;

    private Date utcDate;

    private transient Object oldValue;

    private transient boolean inImmediatePropertyChangeCommit;

    public DomainTransformEvent() {
    }

    @Override
    public int compareTo(DomainTransformEvent o) {
        return CommonUtils.compareLongs(getEventId(), o.getEventId());
    }

    public boolean equivalentTo(DomainTransformEvent o) {
        return o != null && objectId == o.objectId
                && (objectId != 0 || objectLocalId == o.objectLocalId)
                && getObjectClass() == o.getObjectClass()
                && valueId == o.valueId
                && (valueId != 0 || valueLocalId == o.valueLocalId)
                && getValueClass() == o.getValueClass()
                && commitType == o.commitType
                && transformType == o.transformType
                && CommonUtils.equalsWithNullEquality(propertyName,
                        o.propertyName)
                && CommonUtils.equalsWithNullEquality(newStringValue,
                        o.newStringValue)
                && CommonUtils.equalsWithNullEquality(newValue, o.newValue);
    }

    public CommitType getCommitType() {
        return this.commitType;
    }

    public long getEventId() {
        return eventId;
    }

    /**
     * Used in case the server replies (with generated ids) didn't reach a
     * gears-enabled client
     * 
     * @return
     */
    public long getGeneratedServerId() {
        return generatedServerId;
    }

    @Lob
    @Transient
    public String getNewStringValue() {
        return this.newStringValue;
    }

    @Transient
    public Object getNewValue() {
        return this.newValue;
    }

    @Transient
    public Class getObjectClass() {
        if (this.objectClass == null) {
            if (this.objectClassName != null && this.objectClassRef == null) {
                objectClassRef = ClassRef.forName(objectClassName);
            }
            if (this.objectClassRef != null) {
                this.objectClass = this.objectClassRef.getRefClass();
            }
            if (this.objectClass == null && this.objectClassName != null) {
                this.objectClass = Reflections.classLookup()
                        .getClassForName(this.objectClassName);
            }
        }
        return this.objectClass;
    }

    @Transient
    public String getObjectClassName() {
        if (this.objectClassName == null) {
            Class clazz = getObjectClass();
            if (clazz != null) {
                this.objectClassName = clazz.getName();
            }
        }
        return this.objectClassName;
    }

    @Transient
    @JsonIgnore
    public ClassRef getObjectClassRef() {
        if (this.objectClassRef == null && this.objectClass != null) {
            this.objectClassRef = ClassRef.forClass(objectClass);
        }
        return this.objectClassRef;
    }

    public long getObjectId() {
        return this.objectId;
    }

    public long getObjectLocalId() {
        return this.objectLocalId;
    }

    public Integer getObjectVersionNumber() {
        return objectVersionNumber;
    }

    @Transient
    public Object getOldValue() {
        return this.oldValue;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    @Transient
    @JsonIgnore
    public HasIdAndLocalId getSource() {
        return this.source;
    }

    public TransformType getTransformType() {
        return this.transformType;
    }

    public Date getUtcDate() {
        return utcDate;
    }

    @Transient
    public Class getValueClass() {
        if (this.valueClass == null) {
            if (this.valueClassName != null && this.valueClassRef == null) {
                valueClassRef = ClassRef.forName(valueClassName);
            }
            if (this.valueClassRef != null) {
                this.valueClass = this.valueClassRef.getRefClass();
            }
            if (this.valueClass == null && this.valueClassName != null
                    && !this.valueClassName.equals("null")) {
                this.valueClass = Reflections.classLookup()
                        .getClassForName(this.valueClassName);
            }
        }
        return this.valueClass;
    }

    @Transient
    public String getValueClassName() {
        if (this.valueClassName == null && this.valueClass != null) {
            this.valueClassName = this.valueClass.getName();
        }
        return this.valueClassName;
    }

    @Transient
    @JsonIgnore
    public ClassRef getValueClassRef() {
        if (this.valueClassRef == null && this.valueClass != null) {
            this.valueClassRef = ClassRef.forClass(valueClass);
        }
        return this.valueClassRef;
    }

    public long getValueId() {
        return this.valueId;
    }

    public long getValueLocalId() {
        return this.valueLocalId;
    }

    public Integer getValueVersionNumber() {
        return valueVersionNumber;
    }

    @Transient
    public boolean isInImmediatePropertyChangeCommit() {
        return this.inImmediatePropertyChangeCommit;
    }

    public boolean provideIsIdEvent(Class clazz) {
        return objectClass == clazz && "id".equals(propertyName);
    }

    /*
     * this version handles equality for deleted objects
     */
    public boolean provideSourceEquals(HasIdAndLocalId hili) {
        if (hili == null) {
            return false;
        }
        return hili.getClass().equals(objectClass)
                && hili.getLocalId() == objectLocalId
                && hili.getId() == objectId;
    }

    public HasIdAndLocalId provideSourceOrMarker() {
        HasIdAndLocalId source = getSource();
        if (source == null && getObjectLocalId() != 0) {
            HasIdAndLocalId hili = (HasIdAndLocalId) Reflections.classLookup()
                    .newInstance(getObjectClass());
            source = hili;
            hili.setId(getObjectId());
            hili.setLocalId(getObjectLocalId());
        }
        return source;
    }

    /*
     * only used for removing existing transforms, it's not the real object
     */
    public HasIdAndLocalId provideTargetMarkerForRemoval() {
        if (valueId != 0 || valueLocalId != 0) {
            HasIdAndLocalId hili = (HasIdAndLocalId) Reflections.classLookup()
                    .newInstance(getValueClass());
            hili.setId(valueId);
            hili.setLocalId(valueLocalId);
            return hili;
        } else {
            return null;
        }
    }

    public boolean related(DomainTransformEvent itrEvent) {
        if (transformType == TransformType.DELETE_OBJECT) {
            if (itrEvent
                    .getTransformType() == TransformType.REMOVE_REF_FROM_COLLECTION
                    && itrEvent.getValueClass() == getObjectClass()
                    && itrEvent.getValueId() == getObjectId()
                    && itrEvent.getValueLocalId() == getObjectLocalId()) {
                return true;
            }
            if (itrEvent.getTransformType() == TransformType.NULL_PROPERTY_REF
                    && itrEvent.getObjectClass() == getObjectClass()
                    && itrEvent.getObjectId() == getObjectId()
                    && itrEvent.getObjectLocalId() == getObjectLocalId()) {
                Class type = Reflections.propertyAccessor()
                        .getPropertyType(getObjectClass(), getPropertyName());
                return !CommonUtils.isStandardJavaClass(type);
            }
        }
        return false;
    }

    public void setCommitType(CommitType commitType) {
        this.commitType = commitType;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public void setGeneratedServerId(long generatedServerId) {
        this.generatedServerId = generatedServerId;
    }

    public void setInImmediatePropertyChangeCommit(
            boolean inImmediatePropertyChangeCommit) {
        this.inImmediatePropertyChangeCommit = inImmediatePropertyChangeCommit;
    }

    public void setNewStringValue(String newStringValue) {
        this.newStringValue = newStringValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public void setObjectClass(Class objectClass) {
        this.objectClass = objectClass;
        this.objectClassRef = (objectClass == null) ? null
                : ClassRef.forClass(objectClass);
        if (objectClass != null && objectClassRef == null
                && !LooseContext.is(CONTEXT_IGNORE_UNHANDLED_DOMAIN_CLASSES)
                && !TransformManager.get()
                        .isIgnoreUnrecognizedDomainClassException()) {
            throw new UnrecognizedDomainClassException(objectClass);
        }
        this.objectClassName = objectClass == null ? null
                : objectClass.getName();
    }

    public void setObjectClassName(String objectClassName) {
        this.objectClassName = objectClassName;
    }

    public void setObjectClassRef(ClassRef objectClassRef) {
        this.objectClassRef = objectClassRef;
    }

    public void setObjectId(long id) {
        this.objectId = id;
    }

    public void setObjectLocalId(long localId) {
        this.objectLocalId = localId;
    }

    public void setObjectVersionNumber(Integer objectVersionNumber) {
        this.objectVersionNumber = objectVersionNumber;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setSource(HasIdAndLocalId source) {
        this.source = source;
    }

    public void setTransformType(TransformType transformType) {
        this.transformType = transformType;
    }

    public void setUtcDate(Date utcDate) {
        this.utcDate = utcDate;
    }

    public void setValueClass(Class valueClass) {
        this.valueClass = valueClass;
        this.valueClassRef = (valueClass == null) ? null
                : ClassRef.forClass(valueClass);
        if (valueClass != null && valueClassRef == null
                && !LooseContext.is(CONTEXT_IGNORE_UNHANDLED_DOMAIN_CLASSES)
                && !TransformManager.get()
                        .isIgnoreUnrecognizedDomainClassException()) {
            throw new UnrecognizedDomainClassException(valueClass);
        }
        this.valueClassName = valueClass == null ? null : valueClass.getName();
    }

    public void setValueClassName(String valueClassName) {
        this.valueClassName = valueClassName;
    }

    public void setValueClassRef(ClassRef valueClassRef) {
        this.valueClassRef = valueClassRef;
    }

    public void setValueId(long valueId) {
        this.valueId = valueId;
    }

    public void setValueLocalId(long valueLocalId) {
        this.valueLocalId = valueLocalId;
    }

    @Transient
    public void setValueVersionNumber(Integer valueVersionNumber) {
        this.valueVersionNumber = valueVersionNumber;
    }

    public String toDebugString() {
        try {
            LooseContext.pushWithTrue(
                    DTRProtocolSerializer.CONTEXT_EXCEPTION_DEBUG);
            return toString();
        } finally {
            LooseContext.pop();
        }
    }

    @Override
    public String toString() {
        String serialize = new DTRProtocolSerializer().serialize(this);
        return serialize;
    }
}