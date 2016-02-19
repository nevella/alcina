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
package cc.alcina.framework.entity.domaintransform;

import java.sql.ResultSet;
import java.util.Date;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;

@MappedSuperclass
/**
 *
 * @author Nick Reddel
 */
public abstract class DomainTransformEventPersistent
		extends DomainTransformEvent implements HasId {
	private long id;

	private DomainTransformRequestPersistent domainTransformRequestPersistent;

	private Date serverCommitDate;

	// persistence in app subclass
	@Transient
	public DomainTransformRequestPersistent
			getDomainTransformRequestPersistent() {
		return domainTransformRequestPersistent;
	}

	@Transient
	public long getId() {
		return this.id;
	}

	public Date getServerCommitDate() {
		return serverCommitDate;
	}

	@Transient
	public abstract IUser getUser();

	public void setDomainTransformRequestPersistent(
			DomainTransformRequestPersistent domainTransformRequestPersistent) {
		this.domainTransformRequestPersistent = domainTransformRequestPersistent;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setServerCommitDate(Date serverCommitDate) {
		this.serverCommitDate = serverCommitDate;
	}

	public abstract void setUser(IUser user);

	public abstract void wrap(DomainTransformEvent evt);

	/**
	 * Important: if the non-persistent event is to be used, make sure the
	 * object/value localIds are set to 0 or else all hell may break loose
	 */
	public DomainTransformEvent toNonPersistentEvent(boolean clearLocalIds) {
		DomainTransformEvent event = new DomainTransformEvent();
		ResourceUtilities.copyBeanProperties(this, event, null, true);
		if (clearLocalIds) {
			event.setObjectLocalId(0);
			event.setValueLocalId(0);
		}
		// this is purely decorative, so client reflection can show the server
		// id of the transform (since raw DTE has no id field)
		event.setEventId(getId());
		if (event.getUtcDate() == null) {
			event.setUtcDate(serverCommitDate);
		}
		return event;
	}

	public void clearForSimplePersistence() {
		setDomainTransformRequestPersistent(
				Registry.impl(JPAImplementation.class).getInstantiatedObject(
						getDomainTransformRequestPersistent()));
		getDomainTransformRequestPersistent().clearForSimplePersistence();
		setUser(null);
		setSource(null);
		getObjectClass();
		getValueClass();
		setObjectClassRef(null);
		setValueClassRef(null);
	}

	public Date provideBestDate() {
		return serverCommitDate != null ? serverCommitDate : getUtcDate();
	}

	public void copyFromNonPersistentEvent(DomainTransformEvent event) {
		ResourceUtilities.copyBeanProperties(event, this, null, true);
		if (event.getTransformType() == TransformType.CREATE_OBJECT) {
			setGeneratedServerId(event.getObjectId());
		}
	}

	public void fromResultSet(ResultSet rs) throws Exception {
		setNewStringValue(rs.getString("newStringValue"));
		setPropertyName(rs.getString("propertyname"));
		setUtcDate(rs.getTimestamp("utcdate"));
		setObjectId(rs.getLong("objectid"));
		setObjectLocalId(rs.getLong("objectlocalid"));
		setValueId(rs.getLong("valueid"));
		setValueLocalId(rs.getLong("valuelocalid"));
		setEventId(rs.getLong("eventid"));
		setId(rs.getLong("id"));
		setServerCommitDate(rs.getTimestamp("servercommitdate"));
		setGeneratedServerId(rs.getLong("generatedserverid"));
		setObjectClassRef(ClassRef.forId(rs.getLong("objectClassRef_id")));
		setValueClassRef(ClassRef.forId(rs.getLong("valueclassref_id")));
		setObjectVersionNumber(rs.getInt("objectversionnumber"));
		setValueVersionNumber(rs.getInt("valueversionnumber"));
		int i = rs.getInt("transformtype");
		TransformType tt = rs.wasNull() ? null
				: TransformType.class.getEnumConstants()[i];
		setTransformType(tt);
		i = rs.getInt("committype");
		CommitType ct = rs.wasNull() ? null
				: CommitType.class.getEnumConstants()[i];
		setCommitType(ct);
	}

	public static class DomainTransformFromPersistentConverter implements
			Converter<DomainTransformEventPersistent, DomainTransformEvent> {
		@Override
		public DomainTransformEvent
				convert(DomainTransformEventPersistent original) {
			return original.toNonPersistentEvent(true);
		}
	}
}
