package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Comparator;
import java.util.Date;
import java.util.stream.IntStream;

import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.persistence.mvcc.MvccAccess.MvccAccessType;

public class MvccTestEntity extends MvccTestEntityBase<MvccTestEntity> {
	public static final transient Comparator<MvccTestEntity> PRECEDENCE_COMPARATOR = new Comparator<MvccTestEntity>() {
		@Override
		public int compare(MvccTestEntity o1, MvccTestEntity o2) {
			String assign = o1.disallowedInnerAccessField;
			return CommonUtils.compareWithNullMinusOne(
					o1.disallowedInnerAccessField,
					o2.disallowedInnerAccessField);
		}
	};

	@SuppressWarnings("unused")
	private long invalidDuplicateFieldName;

	protected String incorrectAccessField;

	private String disallowedInnerAccessField;

	private String disallowedInnerAccessField2;

	public void a1test() {
	}

	private void disallowedInnerAccessMethod(int v) {
		v = 5;
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

	Date invalidInnerClassAccessMethod2(Object resolutionCache) {
		return null;
	}

	@Override
	public void setId(long id) {
		super.setId(id);
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

	public class Inner1 {
		public Inner1() {
		}

		public void invalid_InnerClassOuterFieldAccess() {
			String s = MvccTestEntity.this.disallowedInnerAccessField;
		}

		public void invalid_InnerClassOuterPrivateMethodAccess() {
			disallowedInnerAccessMethod(5);
		}

		public void invalid_InnerClassOuterPrivateMethodRef() {
			IntStream.of(1, 2)
					.forEach(MvccTestEntity.this::disallowedInnerAccessMethod);
		}
	}

	public class InnerStatic1 {
		private Object resolutionCache = new Object();

		@SuppressWarnings("unused")
		private CachingMap<MvccTestEntity, Date> dateLookup = new CachingMap<MvccTestEntity, Date>(
				this::dateLookupLambda);

		public InnerStatic1() {
		}

		private Date dateLookupLambda(MvccTestEntity entity) {
			return entity.invalidInnerClassAccessMethod2(resolutionCache);
		}

		public void invalid_InnerClassOuterFieldAccess() {
			String s = disallowedInnerAccessField2;
		}

		public void invalid_InnerClassOuterPrivateMethodRef() {
			IntStream.of(1, 2)
					.forEach(MvccTestEntity.this::disallowedInnerAccessMethod);
		}
	}
}
