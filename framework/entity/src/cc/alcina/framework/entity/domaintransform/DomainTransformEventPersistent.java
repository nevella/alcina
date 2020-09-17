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

import javax.persistence.EntityManager;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gwt.user.client.rpc.GwtTransient;
import com.totsp.gwittir.client.beans.Converter;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
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
	private DomainTransformRequestPersistent domainTransformRequestPersistent;

	private Date serverCommitDate;

	private long id;

	@GwtTransient
	private ExTransformDbMetadata exTransformDbMetadata;

	public void beforeTransformCommit(EntityManager entityManager) {
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

	// persistence in app subclass
	@Transient
	@JsonIgnore
	public DomainTransformRequestPersistent
			getDomainTransformRequestPersistent() {
		return domainTransformRequestPersistent;
	}

	@Transient
	public ExTransformDbMetadata getExTransformDbMetadata() {
		return exTransformDbMetadata;
	}

	@Override
	@Transient
	public long getId() {
		return this.id;
	}

	public Date getServerCommitDate() {
		return serverCommitDate;
	}

	@Transient
	@JsonIgnore
	public abstract IUser getUser();

	public void populateDbMetadata(DomainTransformEvent event) {
		ExTransformDbMetadata metadata = new ExTransformDbMetadata();
		metadata.fromEvent(event);
		setExTransformDbMetadata(metadata);
	}

	public void populateDbMetadata(Entity persistentSource) {
		ExTransformDbMetadata metadata = new ExTransformDbMetadata();
		metadata.fromEntity(persistentSource);
		setExTransformDbMetadata(metadata);
	}

	public Date provideBestDate() {
		return serverCommitDate != null ? serverCommitDate : getUtcDate();
	}

	public void setDomainTransformRequestPersistent(
			DomainTransformRequestPersistent domainTransformRequestPersistent) {
		this.domainTransformRequestPersistent = domainTransformRequestPersistent;
	}

	public void setExTransformDbMetadata(
			ExTransformDbMetadata exTransformDbMetadata) {
		this.exTransformDbMetadata = exTransformDbMetadata;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	public void setServerCommitDate(Date serverCommitDate) {
		this.serverCommitDate = serverCommitDate;
	}

	public abstract void setUser(IUser user);

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

	public abstract void wrap(DomainTransformEvent evt);

	public static class DomainTransformFromPersistentConverter implements
			Converter<DomainTransformEventPersistent, DomainTransformEvent> {
		@Override
		public DomainTransformEvent
				convert(DomainTransformEventPersistent original) {
			return original.toNonPersistentEvent(true);
		}
	}

	public static class ExTransformDbMetadata {
		private Date objectCreationDate;

		private Date objectLastModificationDate;

		private int version;

		public void applyTo(Entity entity) {
			if (entity instanceof HasVersionNumber) {
				entity.setVersionNumber(version);
			}
			if (entity instanceof IVersionable) {
				IVersionable iVersionable = (IVersionable) entity;
				if (iVersionable.getCreationDate() == null) {
					iVersionable.setCreationDate(objectCreationDate);
				}
				iVersionable
						.setLastModificationDate(objectLastModificationDate);
			}
		}

		public void fromEntity(Entity persistentSource) {
			setVersion(persistentSource.getVersionNumber());
			if (persistentSource instanceof IVersionable) {
				IVersionable iVersionable = (IVersionable) persistentSource;
				objectCreationDate = iVersionable.getCreationDate();
				objectLastModificationDate = iVersionable
						.getLastModificationDate();
			}
		}

		// these dates won't *exactly* match those from VersioningEntityListener
		// - but will be close enough
		public void fromEvent(DomainTransformEvent event) {
			Date now = new Date();
			if (event.getObjectLocalId() != 0) {
				setObjectCreationDate(now);
			}
			setObjectLastModificationDate(now);
			setVersion(
					TransformManager.get().getObject(event).getVersionNumber()
							+ 1);
		}

		public Date getObjectCreationDate() {
			return objectCreationDate;
		}

		public Date getObjectLastModificationDate() {
			return objectLastModificationDate;
		}

		public int getVersion() {
			return version;
		}

		public void setObjectCreationDate(Date objectCreationDate) {
			this.objectCreationDate = objectCreationDate;
		}

		public void
				setObjectLastModificationDate(Date objectLastModificationDate) {
			this.objectLastModificationDate = objectLastModificationDate;
		}

		public void setVersion(int version) {
			this.version = version;
		}
	}
}
