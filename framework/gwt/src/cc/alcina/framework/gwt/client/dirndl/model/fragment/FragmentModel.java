package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.collections.NotifyingList;
import cc.alcina.framework.common.client.collections.NotifyingList.Notification;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.event.Events;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.Mutation;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode.FragmentRoot;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNodeOps;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.NodeTransformer.FragmentRootTransformer;

/**
 *
 * <p>
 * A peer to a {@link Model} instance that maintains sync between its child
 * structure and another object tree via localdom mutations
 *
 * <ul>
 * <li>It subscribes to a localdom mutation event source
 * <ul>
 * <li>The event source (like the DOM mutationlistener) collects events and
 * emits as batches on request, rather than per mutation
 * <li>In client code, batch handling is automatic via
 * scheduleFinally()/LocalDom.flush
 * <li>In server code, batch handling must be triggered via a call to
 * LocalDom.flush()
 * <li>Note that a future, better implementation would add on-demand mutation
 * processing - any call which might be affected the results of mutation
 * processing would first trigger a flush
 * </ul>
 * <li>The model converts dommutations into changes to its model structure,
 * which are themselves batched and emitted (on localdom mutation processing
 * completion) as modelmutation event collations.
 * <li>The model also maintains indicies (child models by type, etc)
 * <li>Unknown nodes (including unbound Text nodes) are modelled as
 * FragmentNode.Generic or .Text - to provide later support for model validation
 * <li>Attribute values are synced from dom to model (model to dom is provided
 * by Dirndl). In fact, the whole dom -&gt; model mapping uses a 'reverse
 * dirndl' transformation - currently without support for the more baroque
 * transformations (delegated etc), but that's coming.
 *
 * </ul>
 * <p>
 * TODO
 * <ul>
 * <li>Model structure is maintained *when valid* - no unknown nodes can be
 * present.
 * </ul>
 *
 * <pre>
 * <code>
 - fragmentmodel/mutations
  - since fn mutations don't need reflection in dom (although other listeners may
   be interested in the mutations), mark the mutation events as such
  - for completeness, this shouldn't be necessary - mutations should:
    - preserve model/dom correspondence
    - not be fired if the current dom is equivalent to the union state of the mutations


</code>
 * </pre>
 *
 *
 *
 *
 *
 */
@Feature.Ref(Feature_Dirndl_FragmentModel.class)
public class FragmentModel implements InferredDomEvents.Mutation.Handler,
		LayoutEvents.Bind.Handler, NodeTransformer.Provider {
	public static void withMutating(Runnable runnable) {
		MutationRecord.withFlag(FlagMutating.class, runnable);
	}

	Model rootModel;

	Map<DomNode, NodeTransformer> domNodeTransformer = AlcinaCollections
			.newUnqiueMap();

	ModelMutation currentModelMutation;

	List<Class<? extends FragmentNode>> modelledTypes = new LinkedList<>();

	boolean eventScheduled;

	TransformerMatcher transformerMatcher = new TransformerMatcher(this);

	List<NodeTransformer> matchingTransformers;

	FragmentRoot fragmentRoot;

	public FragmentModel(Model rootModel) {
		this.rootModel = rootModel;
		fragmentRoot = new FragmentRoot(rootModel);
		addDefaultModelledTypes();
	}

	public void addModelled(Class<? extends FragmentNode> type) {
		addModelled(List.of(type));
	}

	/**
	 * types will be added at the start of the modelledTypes list, so will be
	 * checked for a match before the default modelled types
	 */
	public void addModelled(List<Class<? extends FragmentNode>> types) {
		// ensure the types also extend Model (they will, but...you know...make
		// it clear)
		Preconditions.checkArgument(types.stream()
				.allMatch(t -> Reflections.isAssignableFrom(Model.class, t)));
		for (int idx = types.size() - 1; idx >= 0; idx--) {
			modelledTypes.add(0, types.get(idx));
		}
	}

	@Override
	public NodeTransformer createNodeTransformer(DomNode node) {
		return transformerMatcher.createNodeTransformer(node);
	}

	public void domToModel() {
		DirectedLayout.Node layoutNode = rootModel.provideNode();
		DomNode node = layoutNode.getRendered().asDomNode();
		FragmentRootTransformer transformer = new NodeTransformer.FragmentRootTransformer(
				layoutNode);
		domNodeTransformer.put(node, transformer);
		currentModelMutation = new ModelMutation(this);
		addDescent(node);
		scheduleEmitMutationEvent();
	}

	/*
	 * Note that some DOM nodes inserted by transformations (NodeBoundary
	 * contents) will not have a corresponding FragmentNode
	 */
	public FragmentNode getFragmentNode(DomNode node) {
		NodeTransformer transformer = domNodeTransformer.get(node);
		if (transformer == null) {
			return null;
		}
		Model model = transformer.getModel();
		if (model instanceof FragmentNode) {
			return (FragmentNode) model;
		} else {
			return null;
		}
	}

	public FragmentRoot getFragmentRoot() {
		return this.fragmentRoot;
	}

	@Override
	public void onBind(Bind event) {
		/*
		 * initialise the transformer/attach structure with a root
		 */
		if (event.isBound()) {
			domToModel();
			fragmentRoot.addNotificationHandler(this::onNotification);
		}
	}

	@Override
	public void onMutation(Mutation event) {
		currentModelMutation = new ModelMutation(this);
		// sketch
		// collate - changes by node
		for (MutationRecord record : event.records) {
			if (record.hasFlag(FlagMutating.class)) {
				continue;
			}
			Node w3cNode = record.target.w3cNode;
			NodeTransformer targetModel = domNodeTransformer
					.get(DomNode.from(w3cNode));
			// only process records which have an existing targetModel - all
			// others will be processed by subtree mods
			if (targetModel == null) {
				continue;
			}
			currentModelMutation.ensure(w3cNode).add(record);
		}
		currentModelMutation.getData().updateRecords.values()
				.forEach(UpdateRecord::apply);
		scheduleEmitMutationEvent();
	}

	public Resolver provideResolver() {
		return rootModel.provideNode().getResolver();
	}

	// FIXME - probably better done via LocalMutation/ModelMutation.
	public void register(FragmentNode node) {
		DomNode domNode = node.domNode();
		if (!domNodeTransformer.containsKey(domNode)) {
			NodeTransformer transformer = createNodeTransformer(domNode);
			transformer.setLayoutNode(node.provideNode());
			registerTransformer(domNode, transformer);
		}
		node.children().forEach(this::register);
	}

	public DomNode rootDomNode() {
		return rootModel.provideElement().asDomNode();
	}

	protected void addDefaultModelledTypes() {
		addModelled(List.of(FragmentNode.TextNode.class,
				FragmentNode.GenericElement.class,
				FragmentNode.GenericProcessingInstruction.class,
				FragmentNode.GenericComment.class));
	}

	void addDescent(DomNode node) {
		DomNode.DomNodeTraversal traversal = new DomNode.DomNodeTraversal(node);
		traversal.forEach(n -> {
			NodeTransformer transformer = domNodeTransformer.get(n);
			if (transformer == null) {
				NodeTransformer parentTransformer = domNodeTransformer
						.get(n.parent());
				/*
				 * Where the domNode -> model creation, attach to the
				 * DirectedLayout.Node structure occurs
				 */
				transformer = parentTransformer.createChildTransformer(n);
				transformer.apply(parentTransformer.getLayoutNode());
				registerTransformer(n, transformer);
			}
		});
	}

	NodeTransformer deregisterTransformer(DomNode n) {
		NodeTransformer transformer = domNodeTransformer.remove(n);
		return transformer;
	}

	void emitMutationEvent() {
		NodeEvent.Context.fromNode(rootModel.provideNode())
				.dispatch(ModelMutation.class, currentModelMutation.getData());
		currentModelMutation = new ModelMutation();
		eventScheduled = false;
	}

	NodeTransformer registerTransformer(DomNode n,
			NodeTransformer transformer) {
		return domNodeTransformer.put(n, transformer);
	}

	void onNotification(NotifyingList.Notification notification) {
		onChildNodesNotification(fragmentRoot, notification);
	}

	void removeDescent(DomNode node) {
		DomNode.DomNodeTraversal traversal = new DomNode.DomNodeTraversal(node);
		NodeTransformer topTransformer = domNodeTransformer.get(node);
		// FIXME - fm - should this ever be null? can reproduce by deleting
		// from a structured contenteditor
		if (topTransformer == null) {
			return;
		}
		traversal.forEach(n -> {
			// FIXME - fm - possibly do this via removal listen (or FN unbind)
			NodeTransformer transformer = deregisterTransformer(n);
		});
		topTransformer.getLayoutNode().remove(false);
	}

	void scheduleEmitMutationEvent() {
		if (eventScheduled) {
			return;
		}
		events.queue(() -> emitMutationEvent());
		eventScheduled = true;
	}

	public final Events events = new Events();

	public interface Has {
		FragmentModel provideFragmentModel();
	}

	public static class ModelMutation
			extends ModelEvent<ModelMutation.Data, ModelMutation.Handler> {
		public ModelMutation() {
			setModel(new Data());
		}

		public ModelMutation(FragmentModel fragmentModel) {
			this();
			getData().fragmentModel = fragmentModel;
		}

		public void addEntry(FragmentNodeOps parent, Model model, Type type) {
			getData().add(new Entry(parent, model, type));
		}

		@Override
		public void dispatch(ModelMutation.Handler handler) {
			handler.onModelMutation(this);
		}

		public Data getData() {
			return super.getModel();
		}

		UpdateRecord ensure(Node w3cNode) {
			return getData().updateRecords.computeIfAbsent(w3cNode,
					n -> getData().fragmentModel.new UpdateRecord(n));
		}

		public static class Data {
			Map<Node, UpdateRecord> updateRecords = AlcinaCollections
					.newUnqiueMap();

			Set<Model> modifiedOrChildrenModified = AlcinaCollections
					.newUniqueSet();

			public List<Entry> entries = new ArrayList<>();

			FragmentModel fragmentModel;

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder().separator("\n");
				format.appendIfNotBlank(entries);
				return format.toString();
			}

			public void add(Entry entry) {
				entries.add(entry);
			}

			public Set<FragmentNodeOps> provideAffectedFragmentNodes() {
				Set<FragmentNodeOps> result = new LinkedHashSet<>();
				entries.forEach(e -> {
					result.add(e.parent);
					Model m = e.model;
					if (m instanceof FragmentNode) {
						result.add((FragmentNode) m);
					}
				});
				return result;
			}
		}

		public static class Entry {
			public Model model;

			public Type type;

			public FragmentNodeOps parent;

			Entry(FragmentNodeOps parent, Model model, Type type) {
				this.parent = parent;
				this.model = model;
				this.type = type;
			}

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder();
				format.appendPadRight(8, type);
				format.append(NestedName.get(model));
				return format.toString();
			}
		}

		public interface Handler extends NodeEvent.Handler {
			void onModelMutation(ModelMutation event);
		}

		public enum Type {
			ADD, REMOVE, CHANGE
		}
	}

	interface FlagMutating extends MutationRecord.Flag {
	}

	class MinimalAncestorSet {
		Set<org.w3c.dom.Node> nodes = new LinkedHashSet<>();

		void removeDescendants() {
			List<org.w3c.dom.Node> toRemove = new ArrayList<>();
			nodes.forEach(n -> {
				org.w3c.dom.Node cursor = n.getParentNode();
				while (cursor != null && cursor != rootModel.provideNode()
						.getRendered().getNode()) {
					if (nodes.contains(cursor)) {
						toRemove.add(n);
						break;
					}
					cursor = cursor.getParentNode();
				}
			});
			toRemove.forEach(nodes::remove);
		}

		void removeIfContained(Set<Node> contained) {
			List<org.w3c.dom.Node> toRemove = new ArrayList<>();
			nodes.forEach(n -> {
				org.w3c.dom.Node cursor = n;
				while (cursor != null && cursor != rootModel.provideNode()
						.getRendered().getNode()) {
					if (contained.contains(cursor)) {
						toRemove.add(n);
						break;
					}
					cursor = cursor.getParentNode();
				}
			});
			toRemove.forEach(nodes::remove);
		}
	}

	/*
	 * Models the mutations applied to a DOM node, and transforms per-Node
	 * mutations to FragmentNode mutations
	 */
	class UpdateRecord {
		Node node;

		List<MutationRecord> records = new ArrayList<>();

		NodeTransformer transformer;

		UpdateRecord(Node node) {
			this.node = node;
			/*
			 * Always non null (see caller)
			 */
			transformer = domNodeTransformer.get(DomNode.from(node));
		}

		void add(MutationRecord record) {
			records.add(record);
		}

		void apply() {
			boolean updateBindings = records.stream()
					.anyMatch(r -> r.type == MutationRecord.Type.attributes
							|| r.type == MutationRecord.Type.characterData);
			if (updateBindings) {
				/*
				 * simpler than per-specific-change
				 */
				transformer.refreshBindings();
			}
			/*
			 * Due to the possibility of 'add, then move to a wrapping node'
			 * mutation combinations exist, hence this reduction (remove later
			 * removed ops) code
			 */
			MinimalAncestorSet adds = new MinimalAncestorSet();
			MinimalAncestorSet removes = new MinimalAncestorSet();
			records.stream()
					.filter(r -> r.type == MutationRecord.Type.childList)
					.forEach(record -> {
						record.addedNodes.stream().map(MutationNode::w3cNode)
								.forEach(adds.nodes::add);
						record.removedNodes.stream().map(MutationNode::w3cNode)
								.forEach(removes.nodes::add);
						adds.removeDescendants();
						removes.removeDescendants();
					});
			adds.removeIfContained(removes.nodes);
			adds.nodes.stream().map(DomNode::from)
					.forEach(FragmentModel.this::addDescent);
			removes.nodes.stream().map(DomNode::from)
					.forEach(FragmentModel.this::removeDescent);
		}
	}

	public FragmentNode copyExternal(FragmentNode external) {
		FragmentNode copy = Reflections.newInstance(external.getClass());
		copy.copyFromExternal(external);
		return copy;
	}

	public void onChildNodesNotification(FragmentNodeOps parent,
			Notification notification) {
		if (notification.postMutation) {
			currentModelMutation.addEntry(parent,
					((DirectedLayout.Node) notification.delta).getModel(),
					notification.add ? ModelMutation.Type.ADD
							: ModelMutation.Type.REMOVE);
		}
	}

	public void ensureComputedNodes(FragmentNode fragmentNode) {
	}
}
