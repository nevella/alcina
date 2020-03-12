package cc.alcina.framework.entity.entityaccess.mvcc;

import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;

public class MvccTestEntity extends AbstractDomainBase<MvccTestEntity> {
	private long id;

	protected String incorrectAccessField;

	@Override
	public long getId() {
		return this.id;
	}

	public void invalid_Super_usage() {
		MvccTestEntity.super.getLocalId();
	}

	public void invalid_This_AssignExpr() {
		MvccTestEntity invalidInstance = null;
		invalidInstance = this;
	}

	public MvccTestEntity invalid_This_ReturnStmt() {
		return this;
	}

	public void invalid_This_This_BinaryExpr() {
		MvccTestEntity instance = null;
		boolean test = instance == this;
	}

	public void invalid_This_VariableDeclarator() {
		MvccTestEntity invalidInstance = this;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}
}
