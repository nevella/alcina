package cc.alcina.framework.servlet.domain.view;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.view.DomainView;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Response;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent.Transform;
import cc.alcina.framework.common.client.csobjects.view.DomainViewSearchDefinition;
import cc.alcina.framework.common.client.csobjects.view.HasFilteredSelfAndDescendantCount;
import cc.alcina.framework.common.client.csobjects.view.TreePath;
import cc.alcina.framework.common.client.csobjects.view.TreePath.Operation;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.servlet.domain.view.DomainViews.Key;
import cc.alcina.framework.servlet.domain.view.DomainViews.ViewsTask;

/**
 * FIXME - dirndl 1.2 - there's confusion between 'node' and 'model' here -
 * partly driven by the separation of 'node' and 'path' (which really, really
 * helps). But NodeModel -> TreeNode...the model *is* the logical node for all
 * intents n purps
 * 
 */
public class LiveTree {
	private DomainTransformCommitPosition earliestPosition;

	private DomainTransformCommitPosition currentPosition;

	private TreePath<LiveNode> root;

	private Deque<PathChange> modelChanges = new LinkedList<>();

	private TreeMap<DomainTransformCommitPosition, List<Transform>> transactionTransforms = new TreeMap<>();

	private Set<NodeGenerator> indexers = new LinkedHashSet<>();

	private GeneratorContext generatorContext;

	private List<LiveNode> modifiedNodes = new ArrayList<>();

	private List<ChangeListener> changeListeners = new ArrayList<>();

	private DomainView rootEntity;

	private NodeGenerator<? extends DomainView, ?> rootGenerator;

	private Set<String> initialisedNodeFilters = new LinkedHashSet<>();

	/*
	 * FIXME - index - this is possibly a stopgap. Really, no mutable entity in
	 * the tree should not have a generator (this may require generator lookup
	 * optimisations). Primarily because, even if an entity node has no
	 * children, it may have categorising project nodes (e.g. first letter) that
	 * are affected by a change to it. On the other hand, maybe it's the
	 * projections which should listen...
	 * 
	 * In any case, 'entityPaths' causing treepath deltas to fire is a good
	 * first path
	 */
	private Multimap<Entity, List<LiveNode>> entityNodes = new Multimap<>();

	public LiveTree(Key key) {
		earliestPosition = DomainStore.writableStore()
				.getTransformCommitPosition();
		currentPosition = earliestPosition;
		generateTree1(key.request.getRoot().find());
	}

	public void addChangeListener(ViewsTask task) {
		ChangeListener changeListener = new ChangeListener(task, this);
		changeListeners.add(changeListener);
	}

	public void cancelChangeListeners(long clientInstanceId, int waitId) {
		Iterator<ChangeListener> itr = changeListeners.iterator();
		while (itr.hasNext()) {
			ChangeListener next = itr.next();
			if (next.task.handlerData.clientInstanceId == clientInstanceId
					&& next.task.handlerData.request.getWaitId() == waitId) {
				next.task.handlerData.noChangeListeners = true;
				next.run();
				itr.remove();
			}
		}
	}

	public void checkChangeListeners() {
		Iterator<ChangeListener> itr = changeListeners.iterator();
		long expire = System.currentTimeMillis() - getEvictMillis();
		while (itr.hasNext()) {
			ChangeListener next = itr.next();
			if (next.getSince().compareTo(currentPosition) < 0
					|| next.getTime() < expire) {
				next.run();
				itr.remove();
			}
		}
	}

	public Response generateResponse(
			Request<? extends DomainViewSearchDefinition> request) {
		TransformFilter transformFilter = rootGenerator
				.transformFilter(request.getSearchDefinition());
		NodeAnnotator annotator = rootGenerator
				.getAnnotator(request.getSearchDefinition());
		Response response = new Response();
		response.setClearExisting(request.getSince() != null
				&& request.getSince().compareTo(earliestPosition) < 0);
		response.getTransforms()
				.addAll(requestToTransform(request, response, transformFilter));
		response.getTransforms().forEach(annotator::annotate);
		response.setRequest(request);
		response.setPosition(currentPosition);
		response.setSelfAndDescendantCount(request.getTreePath() == null
				? root.provideSelfAndDescendantCount(
						transformFilter.filterKey())
				: root.ensurePath(request.getTreePath())
						.provideSelfAndDescendantCount(
								transformFilter.filterKey()));
		return response;
	}

	public TreePath<LiveNode> getRoot() {
		return this.root;
	}

	public boolean hasDeltasSince(DomainTransformCommitPosition since) {
		return transactionTransforms.tailMap(since, false).entrySet().iterator()
				.hasNext();
	}

	public void index(DomainTransformPersistenceEvent event, boolean add) {
		if (!add) {
			// first phase of two (remove, then add)
			newGeneratorContext();
		}
		indexers.forEach(g -> g.indexTransformPersistenceEvent(event,
				generatorContext, add));
		if (add) {
			processEntityChanges(event);
			if (generatorContext.pathChanged.size() > 0) {
				currentPosition = event.getPosition();
				processEvents();
			}
		}
	}

	public void onInvalidateTree() {
		Iterator<ChangeListener> itr = changeListeners.iterator();
		while (itr.hasNext()) {
			ChangeListener next = itr.next();
			next.task.handlerData.clearExisting = true;
			next.run();
			itr.remove();
		}
	}

	public List<Transform> toTransforms(TreePath<LiveNode> path) {
		List<Transform> result = new ArrayList<>();
		while (path != null) {
			Transform transform = new Transform();
			transform.putPath(path);
			transform.setNode(path.getValue().viewNode);
			transform.setOperation(Operation.INSERT);
			result.add(transform);
			path = path.getParent();
		}
		Collections.reverse(result);
		return result;
	}

	private LiveNode ensureNode(TreePath<LiveNode> path,
			NodeGenerator<?, ?> generator, Object segment) {
		if (path.getValue() == null) {
			LiveNode node = new LiveNode();
			node.segment = segment;
			node.generator = generator;
			if (generator.isIndexer()) {
				generatorContext.createdIndexers.add(generator);
			}
			node.path = path;
			path.setValue(node);
		}
		return path.getValue();
	}

	private void generateTree1(DomainView rootEntity) {
		root = TreePath.root(rootEntity);
		this.rootEntity = rootEntity;
		root.putSortedChildren();
		PathChange change = new PathChange();
		change.operation = Operation.INSERT;
		RootGeneratorFactory rootGeneratorFactory = Registry
				.impl(RootGeneratorFactory.class);
		rootGenerator = rootGeneratorFactory.generatorFor(rootEntity);
		newGeneratorContext();
		generatorContext.treeCreation = true;
		change.path = ensureNode(root, rootGenerator, rootEntity).path;
		modelChanges.add(change);
		processEvents();
		rootGenerator.generationComplete();
	}

	private long getEvictMillis() {
		return ResourceUtilities.getInteger(LiveTree.class, "evictSeconds")
				* TimeConstants.ONE_SECOND_MS;
	}

	private void newGeneratorContext() {
		this.generatorContext = new GeneratorContext();
		generatorContext.root = root;
		generatorContext.rootEntity = rootEntity;
	}

	private void processEntityChanges(DomainTransformPersistenceEvent event) {
		event.getTransformPersistenceToken().getTransformCollation()
				.allEntityCollations().forEach(coll -> {
					Entity entity = coll.getEntity();
					List<LiveNode> nodes = entityNodes.get(entity);
					if (nodes != null) {
						nodes.forEach(n -> {
							PathChange change = new PathChange();
							change.operation = Operation.CHANGE;
							change.path = n.path;
							generatorContext.addPathChange(change);
						});
					}
				});
	}

	/*
	 * 
	 * Phase 1: modelchange collation Transform model changes to path changes.
	 * Use a counter (-1 for remove, +1 for add)
	 * 
	 * A node is add/remove/change if collated change (by path) counter is >0,0
	 * or <0
	 * 
	 * If collated change !=0, remove all collated child path changes (since
	 * children will be regenerated)
	 * 
	 * Phase 2: pathchange cascade Then apply pathChanges - which, if the type
	 * is 'add' may in turn generate child path changes. Loop until exhausted
	 * 
	 * Phase 3: bottom-up generate nodes of dirty paths
	 * 
	 * 
	 */
	private void processEvents() {
		// modelchange collation
		GeneratorContext context = this.generatorContext;
		modelChanges.forEach(context::addPathChange);
		modelChanges.clear();
		LinkedHashSet<TreePath<LiveNode>> pathChanged = context.pathChanged;
		List<TreePath<LiveNode>> toRemove = pathChanged.stream()
				.filter(path -> path.hasAncestorMatching(p -> {
					Operation op = p.getValue().collateOperations();
					return op == Operation.INSERT || op == Operation.REMOVE;
				})).collect(Collectors.toList());
		modifiedNodes.forEach(LiveNode::clearCollatedOperation);
		toRemove.forEach(context::removePathChange);
		// Phase 2 - cascade collated changes. A linked hashset can, after all,
		// function as a FIFO queue.
		root.trace(!context.treeCreation);
		do {
			Iterator<TreePath<LiveNode>> iterator = pathChanged.iterator();
			TreePath<LiveNode> pathChange = iterator.next();
			iterator.remove();
			LiveNode liveNode = pathChange.getValue();
			liveNode.onChange();
			context.ensureInTransactionResult(liveNode);
		} while (pathChanged.size() > 0);
		// Phase 3 - bottom up (re)-generate dirty path content models. Parent
		// content models
		// will be regenerated and added to the transform list if any
		// descendants have changed
		while (context.depthChanged.size() > 0) {
			Entry<Integer, Set<TreePath<LiveNode>>> lastEntry = context.depthChanged
					.lastEntry();
			context.depthChanged.remove(lastEntry.getKey());
			Set<TreePath<LiveNode>> lastPaths = lastEntry.getValue();
			for (TreePath<LiveNode> path : lastPaths) {
				LiveNode liveNode = path.getValue();
				context.collateChildren = liveNode;
				liveNode.generateNode();
				context.ensureInTransactionResult(liveNode);
			}
		}
		List<Transform> result = context.generateTransformResult();
		modifiedNodes.forEach(LiveNode::clearContextData);
		modifiedNodes.clear();
		transactionTransforms.put(currentPosition, result);
		checkChangeListeners();
		context.finish();
		root.trace(false);
	}

	private List<Transform> requestToTransform(
			Request<? extends DomainViewSearchDefinition> request,
			Response response, TransformFilter transformFilter) {
		switch (request.getWaitPolicy()) {
		case RETURN_NODES:
			if (request.getSince() != null
					&& request.getSince().compareTo(currentPosition) < 0) {
				/*
				 * force a client retry - with a delay in case there's a change
				 * storm
				 */
				response.setDelayBeforeReturn(true);
				return new ArrayList<>();
			}
			return requestToTransforms_returnNodes(request, transformFilter);
		case WAIT_FOR_DELTAS:
			return requestToTransforms_returnDeltas(request, transformFilter);
		default:
			throw new UnsupportedOperationException();
		}
	}

	private List<Transform> requestToTransforms_returnDeltas(
			Request<? extends DomainViewSearchDefinition> request,
			TransformFilter transformFilter) {
		List<List<Transform>> since = new ArrayList<>();
		for (Entry<DomainTransformCommitPosition, List<Transform>> entry : transactionTransforms
				.descendingMap().entrySet()) {
			if (entry.getKey().compareTo(request.getSince()) <= 0) {
				break;
			}
			since.add(entry.getValue());
		}
		Collections.reverse(since);
		List<Transform> transforms = since.stream().flatMap(Collection::stream)
				.collect(Collectors.toList());
		transforms.removeIf(t -> !transformFilter.test(t));
		return transforms;
	}

	private List<Transform> requestToTransforms_returnNodes(
			Request<? extends DomainViewSearchDefinition> request,
			TransformFilter transformFilter) {
		List<Transform> result = new ArrayList<>();
		LiveNode node = root.ensurePath(request.getTreePath()).getValue();
		if (node != null) {
			String filterKey = transformFilter.filterKey();
			if (Ax.notBlank(filterKey)
					&& initialisedNodeFilters.add(filterKey)) {
				// apply filter test bottom up to all tree nodes
				// parents will be visisted before children
				Deque<TreePath<LiveNode>> deque = new LinkedList<>();
				Set<TreePath<LiveNode>> childrenVisisted = new LinkedHashSet<>();
				deque.push(node.path);
				while (deque.size() > 0) {
					TreePath<LiveNode> last = deque.peekLast();
					LiveNode cursor = last.getValue();
					if (childrenVisisted.contains(last)) {
						Transform transform = new Transform();
						transform.putPath(cursor.path);
						transform.setNode(cursor.viewNode);
						transform.setOperation(Operation.INSERT);
						transformFilter.test(transform);
						deque.removeLast();
					} else {
						for (TreePath<LiveNode> childPath : last
								.getChildren()) {
							deque.addLast(childPath);
						}
						childrenVisisted.add(last);
					}
				}
			}
			switch (request.getChildren()) {
			case IMMEDIATE_ONLY: {
				{
					Transform transform = new Transform();
					transform.putPath(node.path);
					transform.setNode(node.viewNode);
					transform.setOperation(Operation.INSERT);
					result.add(transform);
				}
				int index = 0;
				for (TreePath<LiveNode> childPath : node.path.getChildren()) {
					{
						Transform transform = new Transform();
						transform.putPath(childPath);
						transform.setNode(childPath.getValue().viewNode);
						transform.setOperation(Operation.INSERT);
						result.add(transform);
					}
				}
				result.removeIf(t -> !transformFilter.test(t));
			}
				break;
			case DEPTH_FIRST: {
				Deque<LiveNode> deque = new LinkedList<>();
				deque.push(node);
				/*
				 * A missing fromOffsetExclusivePath should never occur (since
				 * we only handle pagination requests if the requestor's tx is
				 * current)
				 */
				boolean seenStart = request
						.getFromOffsetExclusivePath() == null;
				while (deque.size() > 0 && result.size() < request.getCount()) {
					LiveNode liveNode = deque.removeFirst();
					Transform transform = new Transform();
					transform.putPath(liveNode.path);
					transform.setNode(liveNode.viewNode);
					transform.setOperation(Operation.INSERT);
					boolean emit = transformFilter.test(transform);
					if (seenStart) {
						if (emit) {
							result.add(transform);
						}
					}
					seenStart |= Objects.equals(liveNode.path.toString(),
							request.getFromOffsetExclusivePath());
					if (emit) {
						// only traverse children if parent matches filter
						Deque<LiveNode> toAddRev = new LinkedList<>();
						for (TreePath<LiveNode> child : liveNode.getPath()
								.getChildren()) {
							toAddRev.add(child.getValue());
						}
						while (toAddRev.size() > 0) {
							deque.push(toAddRev.removeLast());
						}
					}
				}
			}
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}
		return result;
	}

	public class GeneratorContext {
		public List<NodeGenerator> createdIndexers = new ArrayList<>();

		public List<NodeGenerator> removedIndexers = new ArrayList<>();

		public DomainView rootEntity;

		public TreePath<LiveNode> root;

		// This field is used as a queue of changes-to-process. End of
		// event-processing collation uses depthChanged (which is not affected
		// by the queue removals)
		public LinkedHashSet<TreePath<LiveNode>> pathChanged = new LinkedHashSet<>();

		Set<TreePath<LiveNode>> transactionResult = new LinkedHashSet<>();

		public TreeMap<Integer, Set<TreePath<LiveNode>>> depthChanged = new TreeMap<>();

		boolean treeCreation = false;

		public LiveNode collateChildren;

		public GeneratorContext() {
		}

		public TreePath<LiveNode> addChildWithGenerator(LiveNode liveNode,
				Object discriminator, NodeGenerator<?, ?> generator) {
			PathChange change = new PathChange();
			change.operation = Operation.INSERT;
			TreePath<LiveNode> childPath = liveNode.ensureChildPath(this,
					generator, discriminator);
			change.path = ensureNode(childPath, generator, discriminator).path;
			addPathChange(change);
			return childPath;
		}

		public void addPathChange(PathChange change) {
			change.path.getValue().addOperation(change.operation);
			pathChanged.add(change.path);
			depthChanged.computeIfAbsent(change.path.depth(),
					d -> new LinkedHashSet<>()).add(change.path);
		}

		public TreePath<LiveNode> deltaChildWithGenerator(LiveNode liveNode,
				Object discriminator, NodeGenerator<?, ?> generator,
				boolean add) {
			if (add) {
				return addChildWithGenerator(liveNode, discriminator,
						generator);
			} else {
				return removeChild(liveNode, discriminator);
			}
		}

		public void dump() {
			depthChanged.entrySet().stream().map(Entry::getValue)
					.flatMap(Collection::stream)
					.forEach(p -> System.out.format("%-30s %s\n", p,
							p.getValue().collateOperations()));
		}

		public boolean ensureInTransactionResult(LiveNode liveNode) {
			return transactionResult.add(liveNode.path);
		}

		public void finish() {
			indexers.removeAll(removedIndexers);
			indexers.addAll(createdIndexers);
		}

		public List<Transform> generateTransformResult() {
			List<Transform> result = new ArrayList<>();
			if (treeCreation) {
				return result;// not needed (will always walk the tree for first
								// 'get')
			}
			transactionResult.forEach(path -> {
				LiveNode liveNode = path.getValue();
				if (liveNode.isDirty()) {
					Transform transform = new Transform();
					transform.putPath(liveNode.path);
					transform.setNode(liveNode.viewNode);
					transform.setOperation(liveNode.collateOperations());
					transform.setBeforePath(
							liveNode.path.provideSuccessorPath());
					Ax.out(transform);
					result.add(transform);
				}
			});
			return result;
		}

		public void removePathChange(TreePath<LiveNode> path) {
			pathChanged.remove(path);
			depthChanged.remove(path.depth(), path);
		}

		private TreePath<LiveNode> removeChild(LiveNode liveNode,
				Object discriminator) {
			if (liveNode.path.hasChildPath(discriminator)) {
				TreePath<LiveNode> childPath = liveNode.path
						.ensureChildPath(discriminator);
				// childPath.removeFromParent();
				// add the remove operation, will be handled in processevents
				PathChange change = new PathChange();
				change.operation = Operation.REMOVE;
				change.path = childPath.getValue().path;
				addPathChange(change);
				return childPath;
			} else {
				return null;
			}
		}
	}

	public class LiveNode
			implements Comparable<LiveNode>, HasFilteredSelfAndDescendantCount {
		Object segment;

		TreePath<LiveNode> path;

		NodeGenerator generator;

		List<Operation> operations = new ArrayList<>();

		private Operation collatedOperation;

		DomainViewNodeContent<?> viewNode;

		private List<ExceptionChild> exceptionChildren = new ArrayList<>();

		boolean dirty;

		public LiveNode() {
		}

		public void addExceptionChild(Object data, Exception e) {
			ExceptionChild exceptionChild = new ExceptionChild(data, e);
			exceptionChildren.add(exceptionChild);
		}

		@Override
		public int compareTo(LiveNode o) {
			return viewNode.compareTo(o.viewNode);
		}

		public TreePath<LiveNode> ensureChildPath(
				GeneratorContext generatorContext,
				NodeGenerator<?, ?> childGenerator, Object discriminator) {
			SegmentComparable segmentComparable = new SegmentComparable(
					generatorContext, childGenerator, discriminator);
			/*
			 * Supply the comparable on path creation - any other way we run
			 * into the "comparable value changes" bugbear and hide up a tree
			 */
			return path.ensureChild(discriminator, segmentComparable);
		}

		public List<ExceptionChild> getExceptionChildren() {
			return this.exceptionChildren;
		}

		public <P extends NodeGenerator> P getGenerator() {
			return (P) generator;
		}

		public TreePath<LiveNode> getPath() {
			return this.path;
		}

		public Object getSegment() {
			return this.segment;
		}

		public DomainViewNodeContent<?> getViewNode() {
			return this.viewNode;
		}

		@Override
		public int provideSelfAndDescendantCount(Object filter) {
			if (getViewNode() != null
					&& (getViewNode() instanceof HasFilteredSelfAndDescendantCount)) {
				return ((HasFilteredSelfAndDescendantCount) getViewNode())
						.provideSelfAndDescendantCount(filter);
			} else {
				return -1;
			}
		}

		public void
				setExceptionChildren(List<ExceptionChild> exceptionChildren) {
			this.exceptionChildren = exceptionChildren;
		}

		@Override
		public String toString() {
			if (viewNode == null) {
				return Ax.format("%s - %s - %s", path, operations, dirty);
			} else {
				return Ax.format("%s - %s - %s\n\t%s", path, operations, dirty,
						viewNode);
			}
		}

		private void indexInEntityMap(boolean add) {
			Entity entity = provideEntity();
			if (entity == null) {
				return;
			}
			if (add) {
				entityNodes.add(entity, this);
			} else {
				// remove entities reachable from subtree
				Deque<LiveNode> removes = new ArrayDeque<>();
				removes.push(this);
				while (removes.size() > 0) {
					LiveNode node = removes.pop();
					entityNodes.remove(node.provideEntity(), this);
					node.path.getChildren().stream().map(TreePath::getValue)
							.forEach(removes::add);
				}
			}
		}

		protected Entity provideEntity() {
			return viewNode == null ? null : viewNode.getEntity();
		}

		void addOperation(Operation operation) {
			operations.add(operation);
			modifiedNodes.add(this);
		}

		void clearCollatedOperation() {
			collatedOperation = null;
		}

		void clearContextData() {
			collatedOperation = null;
			operations.clear();
			dirty = false;
		}

		Operation collateOperations() {
			if (collatedOperation != null) {
				return collatedOperation;
			}
			int total = 0;
			for (Operation operation : operations) {
				switch (operation) {
				case INSERT:
					total++;
					break;
				case REMOVE:
					total--;
					break;
				case CHANGE:
					break;
				default:
					throw new IllegalArgumentException();
				}
			}
			if (total < 0) {
				collatedOperation = Operation.REMOVE;
			} else if (total == 0) {
				collatedOperation = Operation.CHANGE;
			} else {
				collatedOperation = Operation.INSERT;
			}
			modifiedNodes.add(this);
			return collatedOperation;
		}

		void generateNode() {
			DomainViewNodeContent<?> generatedNode = generator.generate(segment,
					generatorContext);
			dirty = viewNode == null || !GraphProjection
					.nonTransientFieldwiseEqual(generatedNode, viewNode);
			if (dirty) {
				modifiedNodes.add(this);
			}
			viewNode = generatedNode;
			indexInEntityMap(true);
			if (dirty && path.getParent() != null) {
				// give parents a chance to refresh if their children change
				if (generatorContext.ensureInTransactionResult(
						path.getParent().getValue())) {
					PathChange pathChange = new PathChange();
					pathChange.path = path.getParent();
					pathChange.operation = Operation.CHANGE;
					generatorContext.addPathChange(pathChange);
				}
			}
		}

		boolean isDirty() {
			switch (collateOperations()) {
			case INSERT:
			case REMOVE:
				return true;
			case CHANGE:
				return dirty;
			default:
				throw new UnsupportedOperationException();
			}
		}

		void onChange() {
			switch (collateOperations()) {
			case INSERT:
				// will not have a viewNode yet
				// indexInEntityMap(true);
				generator.onTreeAddition(generatorContext, this);
				break;
			case REMOVE:
				indexInEntityMap(false);
				path.removeFromParent();
				if (generator != null) {
					generatorContext.removedIndexers.add(generator);
				}
				break;
			case CHANGE:
				break;
			}
		}

		<T> T typedSegment() {
			return (T) segment;
		}

		/*
		 * Exception that occurred when generating a child of this node
		 */
		public class ExceptionChild {
			private Object data;

			private Exception e;

			public ExceptionChild(Object data, Exception e) {
				this.data = data;
				this.e = e;
			}

			@Override
			public String toString() {
				return Ax.format("%s :: %s :: %s", path.toString(),
						(data instanceof Entity
								? ((Entity) data).toStringEntity()
								: data.toString()),
						CommonUtils.toSimpleExceptionMessage(e));
			}
		}
	}

	public static class NodeAnnotator {
		public void annotate(Transform transform) {
		}
	}

	// <P,I,N> -- Parent, Input segment object, Output node
	public interface NodeGenerator<I, O extends DomainViewNodeContent> {
		public O generate(I in, GeneratorContext context);

		public boolean isIndexer();

		public void onTreeAddition(GeneratorContext context, LiveNode liveNode);

		default void generationComplete() {
		}

		default NodeAnnotator getAnnotator(
				DomainViewSearchDefinition domainViewSearchDefinition) {
			return new NodeAnnotator();
		}

		default void indexTransformPersistenceEvent(
				DomainTransformPersistenceEvent event,
				GeneratorContext generatorContext, boolean add) {
		}

		default TransformFilter
				transformFilter(DomainViewSearchDefinition def) {
			throw new UnsupportedOperationException();
		}
	}

	public interface RootGeneratorFactory {
		NodeGenerator<? extends DomainView, ?>
				generatorFor(DomainView rootEntity);
	}

	public static class SegmentComparable
			implements Comparable<SegmentComparable> {
		private DomainViewNodeContent comparable;

		public SegmentComparable(GeneratorContext context,
				NodeGenerator generator, Object discriminator) {
			this.comparable = generator.generate(discriminator, context);
		}

		@Override
		public int compareTo(SegmentComparable o) {
			return comparable.compareTo(o.comparable);
		}
	}

	public interface TransformFilter extends Predicate<Transform> {
		public String filterKey();

		public boolean test0(Transform transform);

		@Override
		default boolean test(Transform t) {
			if (t.getOperation() == Operation.REMOVE) {
				return true;
			}
			return test0(t);
		}
	}

	static class ChangeListener {
		private ViewsTask task;

		private LiveTree tree;

		private long time;

		public ChangeListener(ViewsTask task, LiveTree tree) {
			this.task = task;
			this.tree = tree;
			this.time = System.currentTimeMillis();
		}

		public DomainTransformCommitPosition getSince() {
			return task.handlerData.request.getSince();
		}

		public long getTime() {
			return this.time;
		}

		public void onChange() {
		}

		public void run() {
			task.handlerData.response = tree
					.generateResponse(task.handlerData.request);
			task.handlerData.response
					.setNoChangeListener(task.handlerData.noChangeListeners);
			task.handlerData.response
					.setClearExisting(task.handlerData.clearExisting);
			task.latch.countDown();
		}
	}

	static class PathChange {
		Operation operation;

		TreePath<LiveNode> path;

		@Override
		public String toString() {
			return Ax.format("%s: %s", path, operation);
		}
	}
}