package cc.alcina.framework.servlet.sync;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.sync.StringKeyProvider;

@Bean
public class SyncPair<T> {
	private KeyedObject<T> left;

	private KeyedObject<T> right;

	private SyncPairAction action = SyncPairAction.MERGE;

	private SyncItemMatch<T> matchRecord;

	public SyncPair() {
	}

	public SyncPair(T leftObject, T rightObject, StringKeyProvider keyProvider,
			SyncPairAction action, SyncItemMatch<T> matchRecord) {
		this.matchRecord = matchRecord;
		if (leftObject != null) {
			left = new KeyedObject(leftObject, keyProvider);
		}
		if (rightObject != null) {
			right = new KeyedObject(rightObject, keyProvider);
		}
		this.action = action;
	}

	public void clearForApplyOnlyResponse(boolean keepLeft) {
		matchRecord = null;
		if (keepLeft) {
			right = null;
		} else {
			left = null;
		}
	}

	public SyncPairAction getAction() {
		return this.action;
	}

	public String getKey() {
		return left != null ? left.getKey() : right.getKey();
	}

	public KeyedObject getLeft() {
		return this.left;
	}

	@AlcinaTransient
	public SyncItemMatch<T> getMatchRecord() {
		return this.matchRecord;
	}

	public Class getPairType() {
		return left != null ? left.getType() : right.getType();
	}

	public KeyedObject getRight() {
		return this.right;
	}

	public T leftObject() {
		return left == null ? null : left.getObject();
	}

	public T rightObject() {
		return right == null ? null : right.getObject();
	}

	public void setAction(SyncPairAction action) {
		this.action = action;
	}

	public void setLeft(KeyedObject left) {
		this.left = left;
	}

	public void setRight(KeyedObject right) {
		this.right = right;
	}

	@Override
	public String toString() {
		return String.format(
				"SyncPair - %-8s: key - %s\n\tleft: %s\n\tright: %s\n", action,
				getKey(), left, right);
	}

	public enum SyncAction {
		CREATE, UPDATE, DELETE;
	}

	public static class SyncInstruction {
		public SyncAction action;

		public String reason;

		public SyncInstruction() {
		}

		public SyncInstruction(SyncAction action, String reason) {
			super();
			this.action = action;
			this.reason = reason;
		}
	}

	public enum SyncPairAction {
		CREATE_LEFT {
			@Override
			public SyncAction getDirectedAction(boolean left) {
				return left ? SyncAction.CREATE : null;
			}
		},
		CREATE_RIGHT {
			@Override
			public SyncAction getDirectedAction(boolean left) {
				return !left ? SyncAction.CREATE : null;
			}
		},
		MERGE {
			@Override
			public SyncAction getDirectedAction(boolean left) {
				return SyncAction.UPDATE;
			}
		},
		DELETE_LEFT {
			@Override
			public SyncAction getDirectedAction(boolean left) {
				return left ? SyncAction.DELETE : null;
			}
		},
		DELETE_RIGHT {
			@Override
			public SyncAction getDirectedAction(boolean left) {
				return !left ? SyncAction.DELETE : null;
			}
		},
		IGNORE {
			@Override
			public SyncAction getDirectedAction(boolean left) {
				return null;
			}
		};

		public abstract SyncAction getDirectedAction(boolean left);
	}
}
