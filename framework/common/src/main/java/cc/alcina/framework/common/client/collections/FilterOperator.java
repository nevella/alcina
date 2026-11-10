package cc.alcina.framework.common.client.collections;

import java.util.Objects;

public enum FilterOperator {
	EQ {
		@Override
		public String operationText() {
			return "=";
		}

		@Override
		public boolean test(Object left, Object right) {
			return Objects.equals(left, right);
		}
	},
	NE {
		@Override
		public String operationText() {
			return "!=";
		}

		@Override
		public boolean test(Object left, Object right) {
			return !Objects.equals(left, right);
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
	},
	CONTAINS {
		@Override
		public String operationText() {
			return "[contains]";
		}
	},
	IN {
		@Override
		public String operationText() {
			return "[in]";
		}
	};

	public boolean test(Object left, Object right) {
		throw new UnsupportedOperationException();
	}

	public abstract String operationText();
}
