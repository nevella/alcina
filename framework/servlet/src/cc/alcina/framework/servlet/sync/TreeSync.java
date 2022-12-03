package cc.alcina.framework.servlet.sync;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceTuple;
import cc.alcina.framework.common.client.util.HasEquivalenceString;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.projection.GraphProjection;

/**
 * Synchronizes the states of two generative trees
 *
 * @author nreddel@barnet.com.au
 *
 */
public class TreeSync<T extends TreeSyncable> {
	private TreeProcess syncProcess;

	private Predicate<Node> exitTest = n -> false;

	private T from;

	private T to;

	Logger logger = LoggerFactory.getLogger(getClass());

	public TreeSync(T from, T to) {
		this.from = from;
		this.to = to;
	}

	public Predicate<Node> getExitTest() {
		return this.exitTest;
	}

	public void process() {
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

	public void setExitTest(Predicate<Node> exitTest) {
		this.exitTest = exitTest;
	}

	private void init() {
		SyncPosition firstPosition = new SyncPosition(from, to);
		syncProcess = new TreeProcess(this);
		Node rootNode = syncProcess.getSelectedNode();
		Node firstPosNode = rootNode.add(firstPosition);
		firstPosNode.select(null);
	}

	public static class Builder<T extends TreeSyncable> {
	}

	/*
	 * Run prior to sync (to say optionally update cached remote values)
	 */
	public interface Preparer<U extends TreeSyncable> {
		default U prepare(U u, boolean from) {
			Context context = new Context();
			ProcessObservers.context().publish(context);
			updateContext(context);
			if (context.skip) {
				return u;
			} else {
				return prepare0(u, from);
			}
		}

		U prepare0(U u, boolean from);

		default void updateContext(Context context) {
			// for transient syncables, skip should be false
		}

		public static class Context implements ProcessObservable {
			public boolean skip = false;
		}
	}

	public static class SyncAction {
		public boolean performed = false;

		public Type type = Type.NO_ACTION;

		public String message;

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
		}
	}

	public interface Syncer<U extends TreeSyncable> {
		SyncAction computeAction(U left, U right);

		void performAction(SyncAction result, U left, U right);

		default SyncAction sync(U left, U right) {
			SyncAction action = computeAction(left, right);
			Context context = new Context();
			ProcessObservers.context().publish(context);
			if (context.skip) {
				//
			} else {
				switch (action.type) {
				case CREATE:
				case DELETE:
				case UPDATE:
					performAction(action, left, right);
					break;
				default:
					//
				}
				action.performed = true;
			}
			return action;
		}

		public static class Context implements ProcessObservable {
			public boolean skip = false;
		}
	}

	public class SyncPosition {
		SyncContainer left;

		SyncContainer right;

		Node node;

		public SyncAction action;

		public SyncPosition(Object fromNode, Object toNode) {
			this.left = new SyncContainer(fromNode, true);
			this.right = new SyncContainer(toNode, false);
		}

		public SyncPosition(SyncContainer fromNode, SyncContainer toNode) {
			this.left = fromNode;
			this.right = toNode;
		}

		// a la dirndl -- obj.field.[collection-index].obj...
		public void process(Node node) {
			this.node = node;
			// may either be a syncable or a container of syncables
			if (isTreeSyncable()) {
				Class clazz = getClazz();
				Optional<Preparer> preparer = Registry.optional(Preparer.class,
						clazz);
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
				if (tuple.left == null) {
					tuple.left = new SyncContainer(null, true).withParent(left);
				}
				if (tuple.right == null) {
					tuple.right = new SyncContainer(null, false)
							.withParent(right);
				}
				SyncPosition childPosition = new SyncPosition(tuple.left,
						tuple.right);
				node.add(childPosition);
			}
		}

		@Override
		public String toString() {
			return Ax.format("[%s :: %s]", left, right);
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
			action = syncer.sync(left.treeSyncable(), right.treeSyncable());
			if (action.message != null && action.performed) {
				logger.info(action.message);
			}
		}

		public class Processed implements ProcessObservable {
			public boolean isLeaf() {
				return node.getChildren().size() == 0;
			}

			public boolean isTreeSyncable() {
				return SyncPosition.this.isTreeSyncable();
			}

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder();
				format.appendPadLeft(depth() - 1, "");
				format.appendPadRight(30 + 10 - depth(), pathSegment());
				format.append(action == null ? "" : action.type);
				return format.toString();
			}
		}
	}

	class SyncContainer implements HasEquivalenceString<SyncContainer> {
		private boolean left;

		Object syncable;

		SyncContainer parent;

		int idx;

		private Field field;

		public SyncContainer(Object syncable, boolean left) {
			this.syncable = syncable;
			this.left = left;
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
				throw new UnsupportedOperationException();
				// return "---";
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
			TreeSyncable updated = preparer.prepare(treeSyncable(), left);
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
				Collection collection = (Collection) syncable;
				for (Object object : collection) {
					SyncContainer child = new SyncContainer(object, left);
					child.parent = this;
					child.idx = idx++;
					result.add(child);
				}
			} else {
				Preconditions.checkState(syncable instanceof TreeSyncable);
				List<Field> allFields = SEUtilities
						.allFields(syncable.getClass());
				for (Field field : allFields) {
					Class<?> type = field.getType();
					boolean ts = TreeSyncable.class.isAssignableFrom(type);
					boolean tsCollection = false;
					if (Collection.class.isAssignableFrom(type)) {
						Type genericType = GraphProjection
								.getGenericType(field);
						if (genericType instanceof ParameterizedType) {
							Type parameterizingType = ((ParameterizedType) genericType)
									.getActualTypeArguments()[0];
							if (parameterizingType instanceof Class) {
								Class parameterizingClass = (Class) parameterizingType;
								tsCollection = TreeSyncable.class
										.isAssignableFrom(parameterizingClass);
							}
						}
					}
					if (ts || tsCollection) {
						SyncContainer child = new SyncContainer(
								field.get(syncable), left);
						child.parent = this;
						child.field = field;
						result.add(child);
					}
				}
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
}
