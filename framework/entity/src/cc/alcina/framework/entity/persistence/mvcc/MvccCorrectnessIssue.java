package cc.alcina.framework.entity.persistence.mvcc;

class MvccCorrectnessIssue {
	MvccCorrectnessIssue.MvccCorrectnessIssueType type;

	String message;

	public MvccCorrectnessIssue() {
	}

	public MvccCorrectnessIssue(
			MvccCorrectnessIssue.MvccCorrectnessIssueType type,
			String message) {
		this.type = type;
		this.message = message;
	}

	enum MvccCorrectnessIssueType {
		Invalid_field_access, This_assignment_unknown, This_AssignExpr,
		This_VariableDeclarator, This_ReturnStmt, This_BinaryExpr,
		/*
		 * super calls are actually fine - we're already resolved Super_usage,
		 */
		InnerClassConstructor, InnerClassOuterFieldAccess,
		InnerClassOuterPrivateMethodAccess, InnerClassOuterPrivateMethodRef,
		Duplicate_field_name;
		boolean isUnknown() {
			return this.toString().contains("unknown");
		}
	}
}