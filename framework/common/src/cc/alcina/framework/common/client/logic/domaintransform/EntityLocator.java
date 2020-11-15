package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/*
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class EntityLocator implements Serializable {
	static final transient long serialVersionUID = 1L;

	public static EntityLocator instanceLocator(Entity entity) {
		return new EntityLocator(entity);
	}

	public static EntityLocator objectLocalLocator(DomainTransformEvent dte) {
		return new EntityLocator(dte.getObjectClass(), 0,
				dte.getObjectLocalId());
	}

	public static EntityLocator objectLocator(DomainTransformEvent dte) {
		return new EntityLocator(dte.getObjectClass(), dte.getObjectId(),
				dte.getObjectLocalId());
	}

	public static EntityLocator parse(String v) {
		if (v == null || v.equals("null")) {
			return new EntityLocator();
		}
		String[] parts = v.split("/");
		return new EntityLocator(
				Reflections.classLookup().getClassForName(parts[2]),
				Long.parseLong(parts[0]), Long.parseLong(parts[1]));
	}

	public static EntityLocator parseShort(Class clazz, String key) {
		String simpleName = key.replaceFirst("(\\S+) - (.+)", "$1");
		long id = Long.parseLong(key.replaceFirst("(\\S+) - (.+)", "$2"));
		Preconditions.checkArgument(clazz.getSimpleName().equals(simpleName));
		return new EntityLocator(clazz, id, 0);
	}

	public static EntityLocator requireIdOrLocalId(Entity entity) {
		Preconditions
				.checkArgument(entity.getId() != 0 || entity.getLocalId() != 0);
		return new EntityLocator(entity);
	}

	public static EntityLocator valueLocator(DomainTransformEvent dte) {
		return dte.getValueClass() != null
				&& (dte.getValueId() != 0 || dte.getValueLocalId() != 0)
						? new EntityLocator(dte.getValueClass(),
								dte.getValueId(), dte.getValueLocalId())
						: null;
	}

	@JsonIgnore
	public Class<? extends Entity> clazz;

	@JsonIgnore
	public long id;

	@JsonIgnore
	public long localId;

	public long clientInstanceId;

	private transient int hash;

	public EntityLocator() {
	}

	public EntityLocator(Class<? extends Entity> clazz, long id, long localId) {
		this.clazz = clazz != null
				? (Class<? extends Entity>) Domain.resolveEntityClass(clazz)
				: null;
		this.id = id;
		this.localId = localId;
		if (id == 0) {
			this.clientInstanceId = PermissionsManager.get()
					.getClientInstanceId();
		}
	}

	private EntityLocator(Entity obj) {
		this.clazz = obj.entityClass();
		this.id = obj.getId();
		this.localId = obj.getLocalId();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EntityLocator) {
			EntityLocator o = (EntityLocator) obj;
			if (localId != 0 && o.localId != 0) {
				return localId == o.localId && clazz == o.clazz;
			}
			return id == o.id && clazz == o.clazz;
		}
		return super.equals(obj);
	}

	public <T extends Entity> T find() {
		return Domain.find(this);
	}

	public Class<? extends Entity> getClazz() {
		return this.clazz;
	}

	public long getClientInstanceId() {
		return this.clientInstanceId;
	}

	public long getId() {
		return this.id;
	}

	public long getLocalId() {
		return this.localId;
	}

	public <E extends Entity> E getObject() {
		return Domain.find(this);
	}

	@Override
	public int hashCode() {
		if (hash == 0) {
			hash = (int) ((id != 0 ? id : localId)
					^ (clazz == null ? 0 : clazz.hashCode())
					^ clientInstanceId);
			if (hash == 0) {
				hash = -1;
			}
		}
		return hash;
	}

	public boolean isLocal() {
		return localId != 0;
	}

	public boolean matches(Entity entity) {
		return entity.toLocator().equals(this);
	}

	public void setClazz(Class<? extends Entity> clazz) {
		this.clazz = (Class<? extends Entity>) Domain.resolveEntityClass(clazz);
	}

	public void setClientInstanceId(long clientInstanceId) {
		this.clientInstanceId = clientInstanceId;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setLocalId(long localId) {
		this.localId = localId;
	}

	public String toIdPairString() {
		return Ax.format("%s/%s", id, localId);
	}

	public String toParseableString() {
		if (clazz == null) {
			return "null";
		}
		return Ax.format("%s/%s/%s", id, localId, clazz.getName());
	}

	public String toRecoverableString(long clientInstanceId) {
		if (id != 0) {
			return toString();
		}
		return Ax.format("%s - %s/%s",
				clazz == null ? "??" : CommonUtils.simpleClassName(clazz),
				localId, clientInstanceId);
	}

	@Override
	public String toString() {
		if (id == 0) {
			long clientInstanceId = CommonUtils
					.lv(PermissionsManager.get().getClientInstanceId());
			return toRecoverableString(clientInstanceId);
		}
		return Ax.format("%s - %s",
				clazz == null ? "??" : CommonUtils.simpleClassName(clazz), id);
	}
}