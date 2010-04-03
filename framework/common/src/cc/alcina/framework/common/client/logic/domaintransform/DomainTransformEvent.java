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
import cc.alcina.framework.common.client.util.SimpleStringParser;


@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */

 public class DomainTransformEvent implements Serializable {
	private static final String TGT = "tgt: ";

	private static final String STRING_VALUE = "string value: ";

	private static final String PARAMS = "params: ";

	private static final String SRC = "src: ";

	public static final String DATA_TRANSFORM_EVENT_MARKER = "\nDataTransformEvent: ";

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

	private long targetVersionNumber;

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

	public long getTargetVersionNumber() {
		return targetVersionNumber;
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

	public void setTargetVersionNumber(long targetVersionNumber) {
		this.targetVersionNumber = targetVersionNumber;
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
		StringBuffer sb = new StringBuffer();
		appendTo(sb);
		return sb.toString();
	}

	// TODO- fix the read newstring (\\\\n would be a prob...)
	public static DomainTransformEvent fromString(String s) {
		DomainTransformEvent dte = new DomainTransformEvent();
		SimpleStringParser p = new SimpleStringParser(s);
		String i = p.read(SRC, ",");
		dte.setObjectClass(classFromName(i));
		dte.setObjectId(p.readLong("", ","));
		dte.setObjectLocalId(p.readLong("", "\n"));
		String pName = p.read(PARAMS, ",");
		dte.setPropertyName(pName.equals("null") ? null : pName);
		dte.setCommitType(CommitType.valueOf(p.read("", ",")));
		// TODO - temporary compat
		if (p.indexOf(",") < p.indexOf("\n")) {
			dte.setTransformType(TransformType.valueOf(p.read("", ",")));
			long utcTime = p.readLong("", "\n");
			dte.setUtcDate(new Date(utcTime));
		} else {
			dte.setTransformType(TransformType.valueOf(p.read("", "\n")));
			dte.setUtcDate(CommonLocator.get().currentUtcDateProvider()
					.currentUtcDate());
		}
		i = p.read(STRING_VALUE, "\n");
		dte.setNewStringValue(i.indexOf("\\") == -1 ? i : unescape(i));
		i = p.read(TGT, ",");
		dte.setValueClass(classFromName(i));
		dte.setValueId(p.readLong("", ","));
		dte.setValueLocalId(p.readLong("", "\n"));
		if (dte.getTransformType() != TransformType.CHANGE_PROPERTY_SIMPLE_VALUE
				&& dte.getNewStringValue().equals("null")) {
			dte.setNewStringValue(null);
		}
		return dte;
	}

	private static String unescape(String s) {
		int idx = 0, x = 0;
		StringBuffer sb = new StringBuffer();
		while ((idx = s.indexOf("\\", x)) != -1) {
			sb.append(s.substring(x, idx));
			char c = s.charAt(idx + 1);
			switch (c) {
			case '\\':
				sb.append("\\");
				break;
			case 'n':
				sb.append("\n");
				break;
			}
			x = idx + 2;
		}
		sb.append(s.substring(x));
		return s.toString();
	}

	private static Class classFromName(String className) {
		if (className == null || className.equals("null")) {
			return null;
		}
		return CommonLocator.get().classLookup().getClassForName(className);
	}

	public void appendTo(StringBuffer sb) {
		String ns = newStringValue == null
				|| (newStringValue.indexOf("\n") == -1 && newStringValue
						.indexOf("\\") != -1) ? newStringValue : newStringValue
				.replace("\\", "\\\\").replace("\n", "\\n");
		sb.append(DATA_TRANSFORM_EVENT_MARKER);
		String newlineTab = "\n\t";
		sb.append(newlineTab);
		sb.append(SRC);
		sb.append(getObjectClass().getName());
		sb.append(",");
		sb.append(SimpleStringParser.longToGwtDoublesToString(objectId));
		sb.append(",");
		sb.append(SimpleStringParser.longToGwtDoublesToString(objectLocalId));
		sb.append(newlineTab);
		sb.append(PARAMS);
		sb.append(propertyName);
		sb.append(",");
		sb.append(commitType);
		sb.append(",");
		sb.append(transformType);
		sb.append(",");
		sb.append(utcDate == null ? System.currentTimeMillis() : utcDate
				.getTime());
		sb.append(newlineTab);
		sb.append(STRING_VALUE);
		sb.append(ns);
		sb.append(newlineTab);
		sb.append(TGT);
		sb.append(getValueClass() == null ? null : getValueClass().getName());
		sb.append(",");
		sb.append(SimpleStringParser.longToGwtDoublesToString(valueId));
		sb.append(",");
		sb.append(SimpleStringParser.longToGwtDoublesToString(valueLocalId));
		sb.append("\n");
	}
}