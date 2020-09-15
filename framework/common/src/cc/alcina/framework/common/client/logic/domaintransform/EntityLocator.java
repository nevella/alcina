package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/*
 */
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

	public Class<? extends Entity> clazz;

	public long id;

	public long localId;

	private int hash;

	public EntityLocator() {
	}

	public EntityLocator(Class<? extends Entity> clazz, long id, long localId) {
		this.clazz = clazz;
		this.id = id;
		this.localId = localId;
	}

	private EntityLocator(Entity obj) {
		this.clazz = obj.getClass();
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

	public long getId() {
		return this.id;
	}

	public <E extends Entity> E getObject() {
		return Domain.find(this);
	}

	@Override
	public int hashCode() {
		if (hash == 0) {
			hash = (id != 0 ? Long.valueOf(id).hashCode()
					: Long.valueOf(localId).hashCode())
					^ (clazz == null ? 0 : clazz.hashCode());
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
		this.clazz = clazz;
	}

	public void setId(long id) {
		this.id = id;
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