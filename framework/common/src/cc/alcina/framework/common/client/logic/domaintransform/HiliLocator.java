package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

public class HiliLocator {
	public static HiliLocator fromDte(DomainTransformEvent dte) {
		return new HiliLocator(dte.getObjectClass(), dte.getObjectId());
	}

	public Class<? extends HasIdAndLocalId> clazz;

	public long id;

	private int hash;

	public HiliLocator() {
	}

	public HiliLocator(Class<? extends HasIdAndLocalId> clazz, long id) {
		this.clazz = clazz;
		this.id = id;
	}

	public HiliLocator(HasIdAndLocalId obj) {
		this.clazz = obj.getClass();
		this.id = obj.getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HiliLocator) {
			HiliLocator o = (HiliLocator) obj;
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
			hash = Long.valueOf(id).hashCode()
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
		return CommonUtils.formatJ("%s - %s", clazz == null ? "??"
				: CommonUtils.simpleClassName(clazz), id);
	}
}