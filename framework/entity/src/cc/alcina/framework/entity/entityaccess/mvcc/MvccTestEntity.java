package cc.alcina.framework.entity.entityaccess.mvcc;

import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;

public class MvccTestEntity extends AbstractDomainBase<MvccTestEntity> {
	private long id;

	protected String incorrectAccessField;

	@Override
	public long getId() {
		return this.id;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}
}
