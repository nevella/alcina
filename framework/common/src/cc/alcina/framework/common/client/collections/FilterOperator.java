package cc.alcina.framework.common.client.collections;

public enum FilterOperator {
	EQ {
		@Override
		public String operationText() {
			return "=";
		}
	},
	NE {
		@Override
		public String operationText() {
			return "!=";
		}
	},
	GT {
		@Override
		public String operationText() {
			return ">";
		}
	},
	LT {
		@Override
		public String operationText() {
			return "<";
		}
	},
	IS_NULL {
		@Override
		public String operationText() {
			return "[is null]";
		}
	},
	IS_NOT_NULL {
		@Override
		public String operationText() {
			return "[is not null]";
		}
	},
	GT_EQ {
		@Override
		public String operationText() {
			return ">=";
		}
	},
	LT_EQ {
		@Override
		public String operationText() {
			return "<=";
		}
	},
	MATCHES {
		@Override
		public String operationText() {
			return "[matches]";
		}
	};
	public abstract String operationText();
}
