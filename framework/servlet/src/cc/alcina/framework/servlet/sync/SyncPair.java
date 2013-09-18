package cc.alcina.framework.servlet.sync;


public class SyncPair<T> {
	private KeyedObject<T> left;

	private KeyedObject<T> right;

	private SyncPairAction action = SyncPairAction.MERGE;

	public Class getPairType() {
		return left != null ? left.getType() : right.getType();
	}

	public SyncPair() {
	}

	public SyncPair(T leftObject, T rightObject,
			StringKeyProvider keyProvider, SyncPairAction action) {
		if (leftObject != null) {
			left = new KeyedObject(leftObject, keyProvider);
		}
		if (rightObject != null) {
			right = new KeyedObject(rightObject, keyProvider);
		}
		this.action = action;
	}

	public KeyedObject getLeft() {
		return this.left;
	}

	public KeyedObject getRight() {
		return this.right;
	}

	public SyncPairAction getAction() {
		return this.action;
	}

	public void setLeft(KeyedObject left) {
		this.left = left;
	}

	public void setRight(KeyedObject right) {
		this.right = right;
	}

	public void setAction(SyncPairAction action) {
		this.action = action;
	}

	@Override
	public String toString() {
		return String.format(
				"SyncPair - %-8s: key - %s\n\tleft: %s\n\tright: %s\n",
				action, getKey(), left, right);
	}

	public String getKey() {
		return left != null ? left.getKey() : right.getKey();
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
		};
		public abstract SyncAction getDirectedAction(boolean left);
	}

	public enum SyncAction {
		CREATE, UPDATE, DELETE;
	}
}
