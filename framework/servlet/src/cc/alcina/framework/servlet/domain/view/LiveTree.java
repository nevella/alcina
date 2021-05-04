package cc.alcina.framework.servlet.domain.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.csobjects.view.DomainView;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Response;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Transform;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.WaitPolicy;
import cc.alcina.framework.common.client.csobjects.view.DomainViewSearchDefinition;
import cc.alcina.framework.common.client.csobjects.view.TreePath;
import cc.alcina.framework.common.client.csobjects.view.TreePath.Operation;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.servlet.domain.view.DomainViews.Key;

public class LiveTree {
	private DomainTransformCommitPosition earliestPosition;

	private DomainTransformCommitPosition currentPosition;

	@SuppressWarnings("unused")
	private SearchPredicate searchPredicate;

	private TreePath<LiveNode> root;

	private Deque<PathChange> modelChanges = new LinkedList<>();

	private Map<DomainTransformCommitPosition, List<Transform>> transactionTransforms = new TreeMap<>();

	public LiveTree(Key key) {
		earliestPosition = DomainStore.writableStore()
				.getTransformCommitPosition();
		currentPosition = earliestPosition;
		searchPredicate = new SearchPredicate(
				key.request.getSearchDefinition());
		generateTree1(key.request.getRoot().find());
	}

	public Response generateResponse(
			Request<? extends DomainViewSearchDefinition> request) {
		Response response = new Response();
		response.setClearExisting(request.getSince() == null
				|| request.getSince().compareTo(earliestPosition) < 0);
		response.getTransforms().addAll(requestToTransform(request));
		return response;
	}

	private LiveNode ensureNode(TreePath<LiveNode> path,
			NodeGenerator<?, ?, ?> generator, Object segment) {
		if (path.getValue() == null) {
			LiveNode node = new LiveNode();
			node.segment = segment;
			node.generator = generator;
			node.path = path;
			path.setValue(node);
		}
		return path.getValue();
	}

	private void generateTree1(DomainView rootEntity) {
		root = TreePath.root(rootEntity);
		PathChange change = new PathChange();
		change.operation = Operation.INSERT;
		RootGeneratorFactory rootGeneratorFactory = Registry
				.impl(RootGeneratorFactory.class);
		NodeGenerator<?, ? extends DomainView, ?> rootGenerator = rootGeneratorFactory
				.generatorFor(rootEntity);
		change.path = ensureNode(root, rootGenerator, rootEntity).path;
		modelChanges.add(change);
		processEvents();
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
		GeneratorContext context = new GeneratorContext();
		context.root = root;
		modelChanges.forEach(context::addPathChange);
		modelChanges.clear();
		LinkedHashSet<TreePath<LiveNode>> pathChanged = context.pathChanged;
		List<TreePath<LiveNode>> toRemove = pathChanged.stream()
				.filter(path -> path.hasAncestorMatching(p -> {
					Operation op = p.getValue().collateOperations();
					return op == Operation.INSERT || op == Operation.REMOVE;
				})).collect(Collectors.toList());
		pathChanged.forEach(path -> path.getValue().clearCollatedOperation());
		toRemove.forEach(context::removePathChange);
		// Phase 2 - cascade collated changes. A linked hashset can, after all,
		// function as a FIFO queue.
		do {
			Iterator<TreePath<LiveNode>> iterator = pathChanged.iterator();
			TreePath<LiveNode> pathChange = iterator.next();
			iterator.remove();
			LiveNode liveNode = pathChange.getValue();
			liveNode.onChange(context);
			context.ensureInTransactionResult(liveNode);
		} while (pathChanged.size() > 0);
		// Phase 3 - bottom up generate dirty path nodes. These can optionally
		// mark parents as dirty
		while (context.depthChanged.size() > 0) {
			Entry<Integer, Set<TreePath<LiveNode>>> lastEntry = context.depthChanged
					.lastEntry();
			context.depthChanged.remove(lastEntry.getKey());
			Set<TreePath<LiveNode>> lastPaths = lastEntry.getValue();
			for (TreePath<LiveNode> path : lastPaths) {
				LiveNode liveNode = path.getValue();
				liveNode.generateNode(context);
				context.ensureInTransactionResult(liveNode);
			}
		}
		List<Transform> result = context.generateTransformResult();
		transactionTransforms.put(currentPosition, result);
		Ax.out(result);
	}

	private List<Transform> requestToTransform(
			Request<? extends DomainViewSearchDefinition> request) {
		Preconditions
				.checkState(request.getWaitPolicy() == WaitPolicy.RETURN_NODES);
		List<Transform> result = new ArrayList<>();
		LiveNode node = root.path(request.getTreePath()).getValue();
		if (node != null) {
			{
				Transform transform = new Transform();
				transform.setTreePath(request.getTreePath());
				transform.setNode(node.viewNode);
				transform.setOperation(Operation.INSERT);
				result.add(transform);
			}
			switch (request.getChildren()) {
			case IMMEDIATE_ONLY:
				int index = 0;
				for (TreePath<LiveNode> childPath : node.path.getChildren()) {
					{
						Transform transform = new Transform();
						transform.setTreePath(childPath.toString());
						transform.setNode(childPath.getValue().viewNode);
						transform.setOperation(Operation.INSERT);
						transform.setIndex(index++);
						result.add(transform);
					}
				}
			}
		}
		return result;
	}

	public class GeneratorContext {
		public TreePath<LiveNode> root;

		// This field is used as a queue of changes-to-process. End of
		// event-processing collation uses depthChanged (which is not affected
		// by the queue removals)
		public LinkedHashSet<TreePath<LiveNode>> pathChanged = new LinkedHashSet<>();

		Set<TreePath<LiveNode>> transactionResult = new LinkedHashSet<>();

		public SearchPredicate searchPredicate;

		public TreeMap<Integer, Set<TreePath<LiveNode>>> depthChanged = new TreeMap<>();

		public void addChildWithGenerator(LiveNode liveNode,
				Object discriminator, NodeGenerator<?, ?, ?> generator) {
			PathChange change = new PathChange();
			change.operation = Operation.INSERT;
			TreePath childPath = liveNode.path.child(discriminator);
			change.path = ensureNode(childPath, generator, discriminator).path;
			addPathChange(change);
		}

		public void addPathChange(PathChange change) {
			change.path.getValue().operations.add(change.operation);
			pathChanged.add(change.path);
			depthChanged.computeIfAbsent(change.path.depth(),
					d -> new LinkedHashSet<>()).add(change.path);
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

		public List<Transform> generateTransformResult() {
			List<Transform> result = new ArrayList<>();
			transactionResult.forEach(path -> {
				LiveNode liveNode = path.getValue();
				if (liveNode.isDirty()) {
					Transform transform = new Transform();
					transform.setTreePath(liveNode.path.toString());
					transform.setNode(liveNode.viewNode);
					transform.setOperation(liveNode.collateOperations());
					transform.setIndex(liveNode.path.getInitialIndex());
					result.add(transform);
				}
				liveNode.clearCollatedOperation();
			});
			return result;
		}

		public void removePathChange(TreePath<LiveNode> path) {
			pathChanged.remove(path);
			depthChanged.remove(path.depth(), path);
		}
	}

	public static class LiveNode {
		Object segment;

		TreePath<LiveNode> path;

		NodeGenerator generator;

		List<Operation> operations = new ArrayList<>();

		private Operation collatedOperation;

		DomainViewNode<?> viewNode;

		boolean dirty;

		public <P extends NodeGenerator> P getGenerator() {
			return (P) generator;
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
			return collatedOperation;
		}

		void generateNode(GeneratorContext context) {
			DomainViewNode<?> generatedNode = generator.generate(segment,
					context);
			dirty = viewNode == null || !GraphProjection
					.nonTransientFieldwiseEqual(generatedNode, viewNode);
			viewNode = generatedNode;
			if (dirty && path.getParent() != null) {
				// give parents a chance to refresh if their children change
				if (context.ensureInTransactionResult(
						path.getParent().getValue())) {
					PathChange pathChange = new PathChange();
					pathChange.path = path.getParent();
					pathChange.operation = Operation.CHANGE;
					context.addPathChange(pathChange);
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

		void onChange(GeneratorContext context) {
			switch (collateOperations()) {
			case INSERT:
				generator.onTreeAddition(context, this);
				break;
			case REMOVE:
				path.removeFromParent();
				break;
			case CHANGE:
				break;
			}
		}

		<T> T typedSegment() {
			return (T) segment;
		}
	}

	// <P,I,N> -- Parent, Input segment object, Output node
	public interface NodeGenerator<P extends NodeGenerator, I, O extends DomainViewNode> {
		public O generate(I in, GeneratorContext context);

		public void onTreeAddition(GeneratorContext context, LiveNode liveNode);
	}

	public interface RootGeneratorFactory {
		NodeGenerator<?, ? extends DomainView, ?>
				generatorFor(DomainView rootEntity);
	}

	public static class SearchPredicate implements Predicate {
		private DomainViewSearchDefinition searchDefinition;

		public SearchPredicate(DomainViewSearchDefinition searchDefinition) {
			this.searchDefinition = searchDefinition;
		}

		@Override
		public boolean test(Object t) {
			return true;
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