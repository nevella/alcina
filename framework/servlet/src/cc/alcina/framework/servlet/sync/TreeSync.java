package cc.alcina.framework.servlet.sync;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceTuple;
import cc.alcina.framework.common.client.util.HasEquivalenceString;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.common.client.util.ThrowingSupplier;
import cc.alcina.framework.servlet.sync.TreeSync.SyncAction.Type;

/**
 * Synchronizes the states of two generative trees
 *
 * 
 *
 */
public class TreeSync<T extends TreeSyncable> implements ProcessObservable {
	public static TreeSync.SyncContainer createDummyContainer() {
		TreeSync sync = new TreeSync(null, null);
		return sync.createDummyContainer0();
	}

	private TreeProcess syncProcess;

	private Predicate<Node> exitTest = n -> false;

	private Predicate<Class<? extends TreeSyncable>> treeFilter = t -> true;

	private T from;

	private T to;

	Logger logger = LoggerFactory.getLogger(getClass());

	private boolean modificationOccurred;

	public TreeSync(T from, T to) {
		this.from = from;
		this.to = to;
	}

	public Predicate<Node> getExitTest() {
		return this.exitTest;
	}

	public boolean isModificationOccurred() {
		return this.modificationOccurred;
	}

	public void process() {
		try {
			LooseContext.push();
			process0();
		} finally {
			LooseContext.pop();
		}
	}

	public void setExitTest(Predicate<Node> exitTest) {
		this.exitTest = exitTest;
	}

	public TreeSync<T> withTreeFilter(
			Predicate<Class<? extends TreeSyncable>> treeFilter) {
		this.treeFilter = treeFilter;
		return this;
	}

	private SyncContainer createDummyContainer0() {
		TreeSync<T>.SyncPosition position = new SyncPosition(null, null);
		position.left.position = position;
		return position.left;
	}

	private void init() {
		SyncPosition firstPosition = new SyncPosition(from, to);
		syncProcess = new TreeProcess(this);
		Node rootNode = syncProcess.getSelectedNode();
		Node firstPosNode = rootNode.add(firstPosition);
		firstPosNode.select(null);
	}

	private void process0() {
		ProcessObservers.context().observe(new SyncObserver());
		if (syncProcess == null) {
			init();
		}
		Node cursor = syncProcess.getSelectedNode();
		for (;;) {
			if (cursor == null) {
				return;
			}
			cursor.select(null);
			if (exitTest.test(cursor)) {
				return;
			}
			// traverse, syncing
			SyncPosition pos = (TreeSync<T>.SyncPosition) cursor.getValue();
			pos.process(cursor);
			cursor = cursor.next();
		}
	}

	public static class Builder<T extends TreeSyncable> {
	}

	/*
	 * Run prior to sync (to say optionally update cached remote values)
	 *
	 * syncContainer is required, generate a dummy if calling from outside the
	 * process via
	 */
	public interface Preparer<U extends TreeSyncable> {
		default U prepare(TreeSync.SyncContainer syncContainer, U u,
				boolean from) {
			Preconditions.checkNotNull(syncContainer);
			Context context = new Context(syncContainer, u, from);
			ProcessObservers.context().publish(context);
			if (context.syncContainer != null
					&& context.syncContainer.position.skip) {
				return u;
			} else {
				U prepared = prepare0(context, u, from);
				Preconditions.checkNotNull(prepared);
				context.prepared = prepared;
				ProcessObservers.context().publish(context);
				return prepared;
			}
		}

		U prepare0(Context context, U u, boolean from);

		public class Context<U extends TreeSyncable>
				implements ProcessObservable {
			public U prepared;

			public U value;

			public boolean from;

			public TreeSync.SyncContainer syncContainer;

			public Context(TreeSync.SyncContainer syncContainer, U u,
					boolean from) {
				this.syncContainer = syncContainer;
				this.value = u;
				this.from = from;
			}

			public U contextValue() {
				return prepared != null ? prepared : value;
			}

			public void skip() {
				syncContainer.position.skip = true;
			}
		}
	}

	public static class SyncAction {
		public boolean performed = false;

		public Type type = Type.NO_ACTION;

		public String message;

		public SyncAction() {
		}

		public SyncAction(Type type, String message) {
			this.message = message;
			this.type = type;
		}

		@Override
		public String toString() {
			return Ax.format("%s - performed: %s - message: %s", type,
					performed, message);
		}

		/*
		 * the latter three are really about logging/observing:
		 *
		 * WARN=(warn); NO_ACTION=(log)
		 */
		public enum Type {
			CREATE, UPDATE, DELETE, WARN, NO_ACTION;

			public String asResult() {
				switch (this) {
				case WARN:
					return "[Warn]";
				default:
					return CommonUtils.titleCase(Ax.friendly(this)) + "d";
				}
			}

			public boolean performable() {
				switch (this) {
				case CREATE:
				case UPDATE:
				case DELETE:
					return true;
				default:
					return false;
				}
			}
		}
	}

	public class SyncContainer implements HasEquivalenceString<SyncContainer> {
		private boolean left;

		Object syncable;

		SyncContainer parent;

		int idx;

		private Field field;

		public TreeSync<T>.SyncPosition position;

		public SyncContainer(Object syncable, boolean left) {
			this.syncable = syncable;
			this.left = left;
		}

		public <A extends TreeSyncable> A ancestorSyncable() {
			SyncContainer cursor = parent;
			while (true) {
				if (cursor.syncable instanceof TreeSyncable) {
					return (A) cursor.syncable;
				} else {
					cursor = cursor.parent;
					if (cursor == null) {
						return null;
					}
				}
			}
		}

		public <A extends TreeSyncable> A ancestorSyncable(Class<A> clazz) {
			SyncContainer cursor = parent;
			while (true) {
				if (cursor.syncable != null
						&& clazz.isAssignableFrom(cursor.syncable.getClass())) {
					return (A) cursor.syncable;
				} else {
					cursor = cursor.parent;
					if (cursor == null) {
						return null;
					}
				}
			}
		}

		public List<SyncContainer> computeChildren() {
			try {
				return computeChildren0();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		@Override
		public String equivalenceString() {
			if (syncable == null) {
				return null;
			}
			if (syncable instanceof Collection) {
				return field.getName();
			}
			return ((TreeSyncable) syncable).equivalenceString();
		}

		public void prepare(Preparer preparer) {
			if (syncable == null) {
				return;
			}
			TreeSyncable updated = preparer.prepare(this, treeSyncable(), left);
			if (updated != syncable) {
				// replace. but this depends on how we model parent paths
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public String toString() {
			FormatBuilder fb = new FormatBuilder().separator(" : ");
			fb.appendIfNotBlank(field);
			fb.append(syncable);
			return fb.toString();
		}

		public SyncContainer withParent(SyncContainer parent) {
			this.parent = parent;
			return this;
		}

		private void addWithFilter(List<SyncContainer> list,
				SyncContainer child) {
			if (child.isTreeSyncable()) {
				if (!treeFilter
						.test((Class<? extends TreeSyncable>) child.syncable
								.getClass())) {
					treeFilter
							.test((Class<? extends TreeSyncable>) child.syncable
									.getClass());
					return;
				}
			}
			list.add(child);
		}

		private List<SyncContainer> computeChildren0() throws Exception {
			List<SyncContainer> result = new ArrayList<>();
			/*
			 * traverse - if treesyncable, creating nodes for syncable fields
			 * (via type)
			 *
			 * if collection, kids
			 */
			if (syncable == null) {
				return result;
			}
			if (syncable instanceof Collection) {
				int idx = 0;
				Collection<?> collection = (Collection) syncable;
				for (Object object : collection) {
					SyncContainer child = new SyncContainer(object, left);
					child.parent = this;
					child.idx = idx++;
					addWithFilter(result, child);
				}
			} else {
				Preconditions.checkState(syncable instanceof TreeSyncable);
				((TreeSyncable<?>) syncable).provideChildFields(true)
						.forEach(field -> ThrowingRunnable.wrap(() -> {
							Object syncableChild = field.get(syncable);
							Preconditions.checkNotNull(syncableChild);
							SyncContainer child = new SyncContainer(
									syncableChild, left);
							child.parent = this;
							child.field = field;
							addWithFilter(result, child);
						}));
			}
			return result;
		}

		boolean isTreeSyncable() {
			return syncable instanceof TreeSyncable;
		}

		TreeSyncable treeSyncable() {
			return (TreeSyncable) syncable;
		}
	}

	public static interface Syncer<U extends TreeSyncable<?>> {
		default void computeAction(Operation<U> operation, U left, U right,
				SyncAction action) {
			U value = null;
			if (left == null) {
				value = right;
				action.type = Type.DELETE;
			} else if (right == null) {
				value = left;
				action.type = Type.CREATE;
			} else {
				value = left;
				boolean update = updateIfUnequalFields()
						? left.provideChildFields(false)
								.anyMatch(f -> ThrowingSupplier.wrap(() -> {
									Object leftValue = f.get(left);
									Object rightValue = f.get(right);
									return !Objects.equals(leftValue,
											rightValue);
								}))
						: false;
				action.type = update ? Type.UPDATE : Type.NO_ACTION;
			}
			if (action.type.performable()) {
				action.message = Ax.format("%s %s %s", action.type,
						value.getClass().getSimpleName(), value.name());
			}
		}

		default void createRight(Operation<U> operation) {
			try {
				U left = operation.left;
				Constructor<U> constructor = (Constructor<U>) left.getClass()
						.getDeclaredConstructor(new Class[0]);
				constructor.setAccessible(true);
				U right = (U) constructor.newInstance();
				operation.right = right;
				left.provideChildFields(false)
						.filter(f -> f.getAnnotation(
								TreeSyncable.CreateIgnore.class) == null)
						.forEach(f -> ThrowingRunnable.wrap(() -> {
							f.set(right, f.get(left));
						}));
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		default boolean deleteBeforeCreate() {
			return false;
		}

		void performAction(Operation<U> operation, U left, U right,
				SyncAction action);

		default SyncAction sync(TreeSync.SyncPosition syncPosition) {
			Operation<U> operation = new Operation(syncPosition);
			if (operation.left != null && operation.right != null) {
				// casting is necessary, alas
				((TreeSyncable) operation.right)
						.updateFromSyncEquivalent(operation, operation.left);
			}
			computeAction(operation, operation.left, operation.right,
					operation.action);
			ProcessObservers.context().publish(operation);
			SyncAction action = operation.action;
			if (syncPosition.skip) {
				//
			} else {
				switch (action.type) {
				case CREATE:
					createRight(operation);
					((TreeSyncable) operation.right).updateFromSyncEquivalent(
							operation, operation.left);
					// fallthrough
				case DELETE:
				case UPDATE:
					performAction(operation, operation.left, operation.right,
							operation.action);
					break;
				default:
					//
				}
				action.performed = true;
			}
			return action;
		}

		default boolean updateIfUnequalFields() {
			return false;
		}

		public static class Operation<U extends TreeSyncable>
				implements ProcessObservable {
			public U left;

			public U right;

			public SyncAction action = new SyncAction();

			public TreeSync.SyncPosition syncPosition;

			public Operation(TreeSync.SyncPosition syncPosition) {
				this.syncPosition = syncPosition;
				this.left = (U) syncPosition.left.treeSyncable();
				this.right = (U) syncPosition.right.treeSyncable();
			}

			public void skip() {
				syncPosition.skip = true;
			}

			@Override
			public String toString() {
				return FormatBuilder.keyValues("left", left, "right", right,
						"action", action, "syncPosition", syncPosition);
			}
		}
	}

	// naming - SyncPair is used - and this is also a 'position in the sync
	// tree'
	public class SyncPosition {
		// do not perform action
		public boolean skip;

		public SyncContainer left;

		public SyncContainer right;

		Node node;

		public SyncAction action;

		private Type orderingAction = Type.NO_ACTION;

		public SyncPosition(Object fromNode, Object toNode) {
			this.left = new SyncContainer(fromNode, true);
			this.right = new SyncContainer(toNode, false);
		}

		public SyncPosition(SyncContainer fromNode, SyncContainer toNode,
				Type orderingAction) {
			this.left = fromNode;
			this.right = toNode;
			this.orderingAction = orderingAction;
		}

		// a la dirndl -- obj.field.[collection-index].obj...
		public void process(Node node) {
			this.node = node;
			left.position = this;
			right.position = this;
			// may either be a syncable or a container of syncables
			if (isTreeSyncable()) {
				Class clazz = getClazz();
				Optional<Preparer> preparer = Registry.optional(Preparer.class,
						clazz);
				if (left.syncable != null && right.syncable != null
						&& (left.syncable instanceof TreeSyncable)) {
					((TreeSyncable) right.syncable).updateFromSyncEquivalent(
							null, (TreeSyncable) left.syncable);
				}
				preparer.ifPresent(left::prepare);
				preparer.ifPresent(right::prepare);
				Optional<Syncer> o_syncer = Registry.optional(Syncer.class,
						clazz);
				o_syncer.ifPresent(this::sync);
			}
			ProcessObservers.context().publish(new Processed());
			List<SyncContainer> leftChildren = left.computeChildren();
			List<SyncContainer> rightChildren = right.computeChildren();
			List<HasEquivalenceTuple<SyncContainer>> equivalents = HasEquivalenceHelper
					.getEquivalents(leftChildren, rightChildren, true, true);
			for (HasEquivalenceTuple<SyncContainer> tuple : equivalents) {
				Type orderingAction = Type.NO_ACTION;
				if (tuple.left == null) {
					tuple.left = new SyncContainer(null, true).withParent(left);
					orderingAction = Type.DELETE;
				}
				if (tuple.right == null) {
					tuple.right = new SyncContainer(null, false)
							.withParent(right);
					orderingAction = Type.CREATE;
				}
				SyncPosition childPosition = new SyncPosition(tuple.left,
						tuple.right, orderingAction);
				childPosition.skip = skip;
				node.add(childPosition);
			}
			List<Node> children = node.getChildren();
			// field->collection nodes will be size one.
			if (children.size() > 1) {
				/*
				 * Sort by delete op before create op, if appropriate
				 */
				SyncPosition firstChildValue = (SyncPosition) Ax.first(children)
						.getValue();
				Optional<Syncer> o_childSyncer = Registry.optional(Syncer.class,
						firstChildValue.getClazz());
				if (o_childSyncer.map(Syncer::deleteBeforeCreate)
						.orElse(false)) {
					boolean hasDelete = children.stream()
							.anyMatch(n -> ((SyncPosition) n.getValue())
									.deleteBeforeCreateOrdinal() == -1);
					if (hasDelete) {
						children.sort(Comparator
								.comparing(n -> ((SyncPosition) n.getValue())
										.deleteBeforeCreateOrdinal()));
						node.refreshChildIndicies();
					}
				}
			}
		}

		@Override
		public String toString() {
			return Ax.format("[%s :: %s]", left, right);
		}

		private int deleteBeforeCreateOrdinal() {
			switch (orderingAction) {
			case CREATE:
				return 1;
			case DELETE:
				return -1;
			default:
				return 0;
			}
		}

		private Class getClazz() {
			if (left.syncable == null) {
				return right.syncable.getClass();
			}
			if (right.syncable == null) {
				return left.syncable.getClass();
			}
			Preconditions.checkState(
					left.syncable.getClass() == right.syncable.getClass());
			return left.syncable.getClass();
		}

		int depth() {
			return node.depth();
		}

		boolean isTreeSyncable() {
			return left.isTreeSyncable() || right.isTreeSyncable();
		}

		String pathSegment() {
			if (left.field != null) {
				return left.field.getName();
			}
			if (right.field != null) {
				return right.field.getName();
			}
			if (left.syncable != null) {
				return left.syncable.getClass().getSimpleName();
			}
			if (right.syncable != null) {
				return right.syncable.getClass().getSimpleName();
			}
			throw new IllegalStateException();
		}

		void sync(Syncer syncer) {
			action = syncer.sync(this);
			if (action.performed) {
				if (action.message != null) {
					logger.info(action.message);
				}
			}
		}

		public class Processed implements ProcessObservable {
			public int depth() {
				return node.depth();
			}

			public boolean isLeaf() {
				return node.getChildren().size() == 0;
			}

			public boolean isTreeSyncable() {
				return SyncPosition.this.isTreeSyncable();
			}

			public Class<? extends TreeSyncable> rootType() {
				return ((TreeSync) node.root().getValue()).from.getClass();
			}

			public String
					toOutputString(boolean includeNoActionLeafEquivalence) {
				FormatBuilder format = new FormatBuilder();
				format.appendPadLeft(depth() - 1, "");
				format.appendPadRight(30 + 10 - depth(), pathSegment());
				format.appendPadRight(12, action == null ? "" : action.type);
				if ((!isLeaf() || includeNoActionLeafEquivalence)
						&& action != null && Ax.isBlank(action.message)) {
					action.message = left.equivalenceString();
				}
				if (action != null && action.message != null) {
					format.append(" ");
					format.append(action.message);
				}
				return format.toString();
			}

			@Override
			public String toString() {
				return toOutputString(false);
			}
		}
	}

	private class SyncObserver
			implements ProcessObserver<TreeSync.Syncer.Operation> {
		@Override
		public Class<TreeSync.Syncer.Operation> getObservableClass() {
			return TreeSync.Syncer.Operation.class;
		}

		@Override
		public void topicPublished(TreeSync.Syncer.Operation operation) {
			modificationOccurred |= operation.action.performed
					&& operation.action.type.performable();
		}
	}
}
