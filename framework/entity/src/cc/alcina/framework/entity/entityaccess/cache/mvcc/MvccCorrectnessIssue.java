package cc.alcina.framework.entity.entityaccess.cache.mvcc;

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
		invalid_field_access, This_assignment_unknown, This_AssignExpr,
		This_VariableDeclarator, This_ReturnStmt, This_BinaryExpr, Super_usage;
		boolean isUnknown() {
			return this.toString().contains("unknown");
		}
	}
}