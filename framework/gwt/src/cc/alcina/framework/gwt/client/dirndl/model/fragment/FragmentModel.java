package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedNameProvider;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal.W3cNode;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.Mutation;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.NodeTransformer.FragmentRoot;

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
 * <li>In client code, batch handling is automatic via scheduleFinally()
 * <li>In server code, batch handling must be triggered via a call to
 * LocalDom.mutations().emitMutations()
 * </ul>
 * <li>The model converts dommutations into changes to its model structure,
 * which are themselves batched and emitted (on localdom mutation processing
 * completion) as modelmutation event collations.
 * <li>The model also maintains indicies (child models by type, etc)
 * <li>Unknown nodes (including unbound Text nodes) are modelled as
 * FragmentNode.Generic or .Text - to provide later support for model validation
 * <li>Attribute values are synced from dom to model (model to dom is provided
 * by Dirndl)
 *
 * </ul>
 * <p>
 * TODO
 * <ul>
 * <li>Model structure is maintained *when valid* - no unknown nodes can be
 * present.
 *
 * </ul>
 * <p>
 * Note - this class maintains a parent/children model mapping , since complex
 * mutations can make that hard to reconstruct (see {@link SyncMutations})
 *
 *
 * @author nick@alcina.cc
 *
 */
@Feature.Ref(Feature_Dirndl_FragmentModel.class)
public class FragmentModel implements InferredDomEvents.Mutation.Handler,
		LayoutEvents.Bind.Handler, NodeTransformer.Provider {
	Model rootModel;

	Map<Node, NodeTransformer> domNodeTransformer = AlcinaCollections
			.newUnqiueMap();

	ModelMutation currentModelMutation;

	List<Class<? extends FragmentNode>> modelledTypes = new ArrayList<>();

	boolean eventScheduled;

	public FragmentModel(Model rootModel) {
		this.rootModel = rootModel;
		addDefaultModelledTypes();
	}

	/**
	 * types will be added at the start of the modelledTypes list, so will be
	 * checked for a match before the default modelled types
	 */
	public void addModelled(Class<? extends FragmentNode>... types) {
		// ensure the types also extend Model (they will, but...you know...make
		// it clear)
		Preconditions.checkArgument(Arrays.stream(types)
				.allMatch(t -> Reflections.isAssignableFrom(Model.class, t)));
		for (int idx = types.length - 1; idx >= 0; idx--) {
			modelledTypes.add(0, types[idx]);
		}
	}

	@Override
	public NodeTransformer createNodeTransformer(Node w3cNode) {
		Class<? extends FragmentNode> fragmentNodeType = modelledTypes.stream()
				.filter(type -> FragmentNode.provideIsModelFor(w3cNode, type))
				.findFirst().get();
		NodeTransformer transformer = FragmentNode
				.provideTransformerFor(fragmentNodeType);
		transformer.init(w3cNode);
		return transformer;
	}

	@Override
	public void onBind(Bind event) {
		/*
		 * initialise the transformer/attach structure with a root
		 */
		if (event.isBound()) {
			DirectedLayout.Node layoutNode = rootModel.provideNode();
			Node w3cNode = layoutNode.getRendered().getNode();
			FragmentRoot transformer = new NodeTransformer.FragmentRoot(
					layoutNode);
			domNodeTransformer.put(w3cNode, transformer);
			currentModelMutation = new ModelMutation(this);
			addDescent(w3cNode);
			scheduleEmitMutationEvent();
		}
	}

	@Override
	public void onMutation(Mutation event) {
		currentModelMutation = new ModelMutation(this);
		// sketch
		// collate - changes by node
		for (MutationRecord record : event.records) {
			Node w3cNode = record.target.w3cNode;
			NodeTransformer targetModel = domNodeTransformer.get(w3cNode);
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

	protected void addDefaultModelledTypes() {
		addModelled(FragmentNode.Text.class, FragmentNode.Generic.class);
	}

	void addDescent(Node node) {
		W3cNode traversal = new DepthFirstTraversal.W3cNode(node);
		traversal.forEach(n -> {
			NodeTransformer transformer = domNodeTransformer.get(n);
			if (transformer == null) {
				NodeTransformer parentTransformer = domNodeTransformer
						.get(n.getParentNode());
				/*
				 * Where the domNode -> model creation, attach to the
				 * DirectedLayout.Node structure occurs
				 */
				transformer = parentTransformer.createChildTransformer(n);
				transformer.apply(parentTransformer.getLayoutNode());
				currentModelMutation.addEntry(
						transformer.getLayoutNode().getModel(),
						ModelMutation.Type.ADD);
				domNodeTransformer.put(n, transformer);
			}
		});
	}

	void emitMutationEvent() {
		NodeEvent.Context.fromNode(rootModel.provideNode())
				.dispatch(ModelMutation.class, currentModelMutation.getData());
		currentModelMutation = null;
		eventScheduled = false;
	}

	void removeDescent(Node node) {
		W3cNode traversal = new DepthFirstTraversal.W3cNode(node);
		NodeTransformer topTransformer = domNodeTransformer.get(node);
		// FIXME - fm - should this ever be null? can reproduce by deleting
		// from a structured contenteditor
		if (topTransformer == null) {
			return;
		}
		traversal.forEach(n -> {
			NodeTransformer transformer = domNodeTransformer.remove(n);
			currentModelMutation.addEntry(
					transformer.getLayoutNode().getModel(),
					ModelMutation.Type.REMOVE);
		});
		topTransformer.getLayoutNode().remove(false);
	}

	void scheduleEmitMutationEvent() {
		if (eventScheduled) {
			return;
		}
		Scheduler.get().scheduleFinally(() -> emitMutationEvent());
		eventScheduled = true;
	}

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

		public void addEntry(Model model, Type type) {
			getData().entries.add(new Entry(model, type));
		}

		@Override
		public void dispatch(ModelMutation.Handler handler) {
			handler.onModelMutation(this);
		}

		public Data getData() {
			return super.getModel();
		}

		@Override
		public Class<ModelMutation.Handler> getHandlerClass() {
			return ModelMutation.Handler.class;
		}

		UpdateRecord ensure(Node w3cNode) {
			return getData().updateRecords.computeIfAbsent(w3cNode,
					n -> getData().fragmentModel.new UpdateRecord(n));
		}

		public static class Data {
			Map<Node, UpdateRecord> updateRecords = AlcinaCollections
					.newUnqiueMap();

			public List<Entry> entries = new ArrayList<>();

			FragmentModel fragmentModel;

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder().separator("\n");
				format.appendIfNotBlank(entries);
				return format.toString();
			}
		}

		public static class Entry {
			public Model model;

			public Type type;

			Entry(Model model, Type type) {
				this.model = model;
				this.type = type;
			}

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder();
				format.appendPadRight(8, type);
				format.append(NestedNameProvider.get(model));
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

	class UpdateRecord {
		Node node;

		List<MutationRecord> records = new ArrayList<>();

		NodeTransformer transformer;

		UpdateRecord(Node node) {
			this.node = node;
			/*
			 * Always non null (see caller)
			 */
			transformer = domNodeTransformer.get(node);
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
			 * mutation combinations exist, only
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
			adds.nodes.forEach(FragmentModel.this::addDescent);
			removes.nodes.forEach(FragmentModel.this::removeDescent);
		}
	}
}