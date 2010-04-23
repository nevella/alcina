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
import java.util.Date;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.util.CommonUtils;

@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */
public class DomainTransformEvent implements Serializable {
	private String propertyName;

	private transient Object newValue;

	private transient Object oldValue;

	private transient Object source;

	private transient Class objectClass;

	private String objectClassName;

	private String valueClassName;

	private ClassRef objectClassRef;

	private ClassRef valueClassRef;

	private long eventId;

	private long objectId;

	private long objectLocalId;

	private long generatedServerId;

	private long objectVersionNumber;

	private long valueVersionNumber;

	private transient Class valueClass;

	private long valueId;

	private long valueLocalId;

	private String newStringValue;

	private TransformType transformType;

	private CommitType commitType = CommitType.TO_LOCAL_BEAN;

	private Date utcDate;

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
	public String getNewStringValue() {
		return this.newStringValue;
	}

	@Transient
	public Object getNewValue() {
		return this.newValue;
	}

	@Transient
	public Class getObjectClass() {
		if (this.objectClass == null && this.objectClassRef != null) {
			this.objectClass = this.objectClassRef.getRefClass();
		}
		return this.objectClass;
	}

	public String getObjectClassName() {
		return this.objectClassName;
	}

	@Transient
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

	/**
	 * Used to increment the client version number
	 * 
	 * @return
	 */
	public long getObjectVersionNumber() {
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
	public Object getSource() {
		return this.source;
	}
	@Transient
	public long getValueVersionNumber() {
		return valueVersionNumber;
	}

	public TransformType getTransformType() {
		return this.transformType;
	}

	public Date getUtcDate() {
		return utcDate;
	}

	@Transient
	public Class getValueClass() {
		if (this.valueClass == null && this.valueClassRef != null) {
			this.valueClass = this.valueClassRef.getRefClass();
		}
		return this.valueClass;
	}

	public String getValueClassName() {
		if (this.valueClassName == null && this.valueClass != null) {
			this.valueClassName = this.valueClass.getName();
		}
		return this.valueClassName;
	}

	@Transient
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

	public void setCommitType(CommitType commitType) {
		this.commitType = commitType;
	}

	public void setEventId(long eventId) {
		this.eventId = eventId;
	}

	public void setGeneratedServerId(long generatedServerId) {
		this.generatedServerId = generatedServerId;
	}

	public void setNewStringValue(String newStringValue) {
		this.newStringValue = newStringValue;
	}

	public void setNewValue(Object newValue) {
		this.newValue = newValue;
	}

	public void setObjectClass(Class objectClass) {
		this.objectClass = objectClass;
		this.objectClassRef = (objectClass == null) ? null : ClassRef
				.forClass(objectClass);
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

	public void setObjectVersionNumber(long generatedObjectVersionId) {
		this.objectVersionNumber = generatedObjectVersionId;
	}

	public void setOldValue(Object oldValue) {
		this.oldValue = oldValue;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setSource(Object source) {
		this.source = source;
	}
	@Transient
	public void setValueVersionNumber(long valueVersionNumber) {
		this.valueVersionNumber = valueVersionNumber;
	}

	public void setTransformType(TransformType transformType) {
		this.transformType = transformType;
	}

	public void setUtcDate(Date utcDate) {
		this.utcDate = utcDate;
	}

	public void setValueClass(Class valueClass) {
		this.valueClass = valueClass;
		this.valueClassRef = (valueClass == null) ? null : ClassRef
				.forClass(valueClass);
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

	@Override
	public String toString() {
		return new DTRProtocolSerializer().serialize(this);
	}

	public boolean related(DomainTransformEvent itrEvent) {
		if (transformType == TransformType.DELETE_OBJECT) {
			if (itrEvent.getTransformType() == TransformType.REMOVE_REF_FROM_COLLECTION
					&& itrEvent.getValueClass() == getObjectClass()
					&& itrEvent.getValueId() == getObjectId()
					&& itrEvent.getValueLocalId() == getObjectLocalId()) {
				return true;
			}
			if (itrEvent.getTransformType() == TransformType.NULL_PROPERTY_REF
					&& itrEvent.getObjectClass() == getObjectClass()
					&& itrEvent.getObjectId() == getObjectId()
					&& itrEvent.getObjectLocalId() == getObjectLocalId()) {
				Class type=CommonLocator.get().propertyAccessor().getPropertyType(getObjectClass(),getPropertyName());
				return !CommonUtils.isStandardJavaClass(type);
			}
		}
		return false;
	}
}