package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

/*
 */
public class HiliLocator implements Serializable {
	static final transient long serialVersionUID = 1L;

	public static HiliLocator objectLocator(DomainTransformEvent dte) {
		return new HiliLocator(dte.getObjectClass(), dte.getObjectId(),
				dte.getObjectLocalId());
	}

	public static HiliLocator objectLocalLocator(DomainTransformEvent dte) {
		return new HiliLocator(dte.getObjectClass(), 0, dte.getObjectLocalId());
	}

	public static HiliLocator valueLocator(DomainTransformEvent dte) {
		return dte.getValueClass() != null && (dte.getValueId() != 0)
				? new HiliLocator(dte.getValueClass(), dte.getValueId(),
						dte.getValueLocalId())
				: null;
	}

	public Class<? extends HasIdAndLocalId> clazz;

	public long id;

	public long localId;

	private int hash;

	public HiliLocator() {
	}

	public HiliLocator(Class<? extends HasIdAndLocalId> clazz, long id,
			long localId) {
		this.clazz = clazz;
		this.id = id;
		this.localId = localId;
	}

	public HiliLocator(HasIdAndLocalId obj) {
		this.clazz = obj.getClass();
		this.id = obj.getId();
		this.localId = obj.getLocalId();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HiliLocator) {
			HiliLocator o = (HiliLocator) obj;
			if (localId != 0 && o.localId != 0) {
				return localId == o.localId && clazz == o.clazz;
			}
			return id == o.id && clazz == o.clazz;
		}
		return super.equals(obj);
	}

	public Class<? extends HasIdAndLocalId> getClazz() {
		return this.clazz;
	}

	public long getId() {
		return this.id;
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

	public void setClazz(Class<? extends HasIdAndLocalId> clazz) {
		this.clazz = clazz;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("%s - %s",
				clazz == null ? "??" : CommonUtils.simpleClassName(clazz), id);
	}

	public String toRecoverableString(long clientInstanceId) {
		if (id != 0) {
			return toString();
		}
		return CommonUtils.formatJ("%s - %s/%s",
				clazz == null ? "??" : CommonUtils.simpleClassName(clazz),
				localId, clientInstanceId);
	}
}