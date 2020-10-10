package cc.alcina.framework.entity.persistence.mvcc;

import java.util.stream.IntStream;

import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;

public class MvccTestEntity extends MvccTestEntityBase<MvccTestEntity> {
	@SuppressWarnings("unused")
	private long invalidDuplicateFieldName;

	protected String incorrectAccessField;

	private String disallowedInnerAccessField;

	public void a1test() {
	}

	@Override
	public long getId() {
		return this.id;
	}

	public Inner1 invalid_InnerConstructor() {
		return new Inner1();
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

	/*
	 * normally would
	 * use @MvccAccessCorrect(type=MvccAccessType.RESOLVE_TO_DOMAIN_IDENTITY) -
	 * which would wrap the constructor. This demonstrates the actual wrapping
	 */
	@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
	public Inner1 valid_InnerConstructor() {
		if (this == domainIdentity()) {
			return new Inner1();
		} else {
			return domainIdentity().valid_InnerConstructor();
		}
	}

	private void disallowedInnerAccessMethod(int v) {
		v = 5;
	}

	public class Inner1 {
		public Inner1() {
		}

		public void invalid_InnerClassOuterFieldAccess() {
			String s = disallowedInnerAccessField;
		}

		public void invalid_InnerClassOuterPrivateMethodAccess() {
			disallowedInnerAccessMethod(5);
		}

		public void invalid_InnerClassOuterPrivateMethodRef() {
			IntStream.of(1, 2)
					.forEach(MvccTestEntity.this::disallowedInnerAccessMethod);
		}
	}
}
