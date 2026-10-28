package cc.alcina.framework.servlet.transform.fragment.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;

import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode.FilteringOutputTreeChildren;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.FragmentDocumentOverlayToken;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.FragmentDocumentOverlayToken.Transformer;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.Containment;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.MeasureTreeComparator;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * <p>
 * A (the) key transformer class
 *
 * <ol>
 * <li>Sorts measures as a depth-first tree (the initial output tree)
 * <li>Traverse, emitting corresponding MeasureTree nodes, which link
 * MeasureSelections (analysed input) to FragmentDocumentNodes (output)
 *
 * <li>From this point on, containment is a function of position in the output
 * tree, not the source (measurecontainment) tree
 * </ol>
 * 
 * <p>
 * Note that the inputs will already be in a containment structure with no
 * overlaps (that's required for deterministic behaviour for this layer)
 * 
 * <p>
 * Note there are several ways to omit incoming MeasureSelections from output -
 * if the type extends NonOutput, or if its omit field is set, or if the
 * selection has a NonOutputTree ancestor. See the
 * cc.alcina.framework.servlet.transform.fragment package doc for more detail
 *
 */
public class MeasureOutput extends Layer<MeasureSelection>
		implements Layer.Output {
	public static FragmentDocumentNode render(MeasureSelection selection) {
		Class<? extends FragmentDocumentOverlayToken> tokenClass = (Class<? extends FragmentDocumentOverlayToken>) selection
				.get().token.getClass();
		/*
		 * Note that this does not use any additional external context (yet) -
		 * whatever context is required should be encapsulated in the
		 * MeasureSelection instance.
		 */
		Transformer<?, ?> transformer = Registry.impl(
				FragmentDocumentOverlayToken.Transformer.class, tokenClass);
		return transformer.apply(selection);
	}

	public static Class<? extends FragmentDocumentNode>
			getOutputType(MeasureSelection selection) {
		Class<? extends FragmentDocumentOverlayToken> tokenClass = (Class<? extends FragmentDocumentOverlayToken>) selection
				.get().token.getClass();
		Class<? extends Transformer> transformerType = Registry
				.query(FragmentDocumentOverlayToken.Transformer.class)
				.addKeys(tokenClass).registration();
		return Transformer.to((Class<Transformer<?, ?>>) transformerType);
	}

	MeasureTree tree;

	Token.Order order;

	MeasureContainment measureContainment;

	@Override
	public void onBeforeTraversal(SelectionTraversal.State traversalState) {
		super.onBeforeTraversal(traversalState);
		order = state.traversalContext(Order.Has.class).getOrder();
	}

	@Override
	public Collection<MeasureSelection> computeInputs() {
		List<MeasureSelection> filteredSelections = state.traversalState.selections
				.get(inputType, true).stream()
				.filter(m -> !m.isOmit()
						&& !(m instanceof MeasureSelection.Intermediate))
				.toList();
		/*
		 * caused by complex truncations - FIXME - e.g. Environment Protection
		 * and Biodiversity Conservation Act 1999. Will require some more
		 * truncation debugging tools/thinkin
		 */
		Set<Measure> unique = Registry
				.impl(CollectionCreators.HashSetCreator.class).create();
		filteredSelections.forEach(sel -> {
			Measure measure = sel.get();
			if (!unique.add(measure)) {
				logger.warn("Omitting duplicate measure: {}", measure);
				sel.setOmit(true);
			}
		});
		Stream<MeasureSelection> filteredSelectionsStream = filteredSelections
				.stream().filter(m -> !m.isOmit());
		MeasureTreeComparator comparator = new MeasureTreeComparator(order);
		List<MeasureSelection> measures = filteredSelectionsStream
				.sorted(comparator).collect(Collectors.toList());
		// debug comparator
		// CommonUtils.validateComparator(measures, comparator);
		// debug - emit a process message (often useful to debug here)
		// measures.forEach(MeasureSelection::toDebugOut);
		measureContainment = new MeasureContainment(order, measures);
		tree = new MeasureTree(this, order);
		return measures;
	}

	@Override
	public void process(MeasureSelection selection) throws Exception {
		// will cause selection of the corresponding created MeasureNode
		tree.add(selection);
	}

	@Override
	protected void onAfterIteration() {
		tree.insertText();
		super.onAfterIteration();
	}

	public class MeasureTree {
		MeasureNode root;

		static Logger logger = org.slf4j.LoggerFactory
				.getLogger(MeasureTree.class);

		/*
		 * the current 'point in the tree' - used as the insertion parent
		 */
		MeasureNode cursor;

		MeasureOutput measureOutput;

		Order order;

		List<MeasureSelection> removedFromOutputTree = new ArrayList<>();

		public MeasureTree(MeasureOutput measureOutput, Token.Order order) {
			this.measureOutput = measureOutput;
			this.order = order;
		}

		int loggedExceptions = 0;

		void add(MeasureSelection selection) {
			// determine parent
			MeasureNode entryCursor = cursor;
			Token token = selection.get().token;
			/*
			 * Because removedFromOutput nodes cannot contain other
			 * removedFromOutput nodes, and this method is called in source
			 * document order, this works (just checking the last element of the
			 * list)
			 */
			if (removedFromOutputTree.size() > 0) {
				MeasureSelection last = Ax.last(removedFromOutputTree);
				if (last.contains(selection, order)) {
					FilteringOutputTreeChildren filteringRendered = filtering
							.get(last);
					if (filteringRendered != null) {
						Class<? extends FragmentDocumentNode> outputType = getOutputType(
								selection);
						if (filteringRendered.allowTreeChild(outputType)) {
							// continue
						} else {
							return;
						}
					} else {
						return;
					}
				}
			}
			Selection<?> parentSelection = selection.parentSelection();
			/*
			 * Adjust the cursor (in the output tree) to be the correct input
			 * location
			 */
			Containment selectionContainment = measureContainment.containments
					.get(selection);
			while (cursor != null) {
				/*
				 * note the containment relation is defined as either text
				 * containment or logical output containment - *not* necessarily
				 * source element containment (although it -can- be that)
				 * 
				 * for instance, indentruns are multiple nested selections
				 * generated from various things (like styles) and they have a
				 * significant containment relationship here
				 * 
				 * So check index/order containment (via measureContainment) and
				 * then text containment (via intpair)
				 */
				Containment cursorContainment = measureContainment.containments
						.get(cursor.measureSelection);
				if (selectionContainment.ancestors(false)
						.anyMatch(ct -> ct == cursorContainment)) {
					/*
					 * cursor is a containment ancestor of selection )
					 */
					break;
				}
				cursor = cursor.treeParent;
			}
			if (cursor instanceof FragmentDocumentNode.Leaf) {
				// this type signifies 'should not have children in the output
				// document'. This should probably not *have* any children, so
				// is more a guard against an upstream issue.
				//
				// FIXME - st.bn - low
				return;
			}
			if (selection.get().getToken() instanceof Token.IgnoreEmptyText
					&& selection.get().provideIsPoint()) {
				/*
				 * e.g. an empty text node. should probably not be emitted
				 * 
				 * // FIXME - st.bn - low
				 */
				return;
			}
			FragmentDocumentNode rendered = MeasureOutput.render(selection);
			if (rendered instanceof FragmentDocumentNode.NonOutput) {
				if (rendered instanceof FragmentDocumentNode.NonOutputTree) {
					removedFromOutputTree.add(selection);
					/*
					 * this renderedtype signifies 'should not be attached to
					 * the output document'
					 */
				}
				return;
			}
			if (rendered instanceof FragmentDocumentNode.NonOutputTreeChildren) {
				removedFromOutputTree.add(selection);
			}
			if (rendered instanceof FragmentDocumentNode.FilteringOutputTreeChildren) {
				// the removal will be selectively applied
				filtering.put(selection,
						(FilteringOutputTreeChildren) rendered);
				removedFromOutputTree.add(selection);
			}
			MeasureNode node = new MeasureNode(selection, cursor, rendered);
			if (cursor == null) {
				Preconditions.checkState(root == null);
				root = node;
				if (!root.get().provideIsBound()) {
					throw new IllegalStateException(
							"Root is not bound to a dirndl layout. Possibly you didn't call link() after building the FragmentDocument");
				}
			} else {
				// dom attach
				FragmentDocumentNode parentFragmentNode = node.treeParent.get();
				try {
					parentFragmentNode.nodes().append(rendered);
				} catch (RuntimeException e) {
					if (loggedExceptions++ < 5) {
						/*
						 * invalid structure (e.g TABLE -> OL)
						 */
						logger.warn("Invalid generated DOM structure");
						MeasureNode parentMeasureNode = new MeasureNode.ReverseLookup()
								.getMeasureNode(measureOutput.getTraversal(),
										parentFragmentNode);
						Measure parentMeasure = parentMeasureNode.measureSelection
								.get();
						logger.warn(
								"Parent input selection for output structural exception: %s - %s :: %s <-++ %s",
								parentMeasure.token, parentMeasure.toIntPair(),
								NestedName.get(parentFragmentNode),
								NestedName.get(rendered));
						parentMeasureNode.measureSelection
								.logContainingSelections(order);
						FragmentDocumentNode grandparentFragmentNode = node.treeParent.treeParent
								.get();
						MeasureNode grandparentMeasureNode = new MeasureNode.ReverseLookup()
								.getMeasureNode(measureOutput.getTraversal(),
										grandparentFragmentNode);
						Measure grandparentMeasure = grandparentMeasureNode.measureSelection
								.get();
						logger.warn(
								"Grandparent input selection for output structural exception: %s - %s :: %s <-++ %s",
								grandparentMeasure.token,
								grandparentMeasure.toIntPair(),
								NestedName.get(parentFragmentNode),
								NestedName.get(rendered));
						grandparentMeasureNode.measureSelection
								.logContainingSelections(order);
						throw e;
					} else {
						return;
					}
				}
			}
			cursor = node;
			measureOutput.select(node);
		}

		Map<MeasureSelection, FragmentDocumentNode.FilteringOutputTreeChildren> filtering = new LinkedHashMap<>();

		/*
		 * Insert text nodes (or fragments) from source document only if the
		 * output node is a leaf. This handles say a source text node containing
		 * multiple catchwords - and thus multiple output elements rather than a
		 * single output text node
		 * 
		 * FIXME - this is a hack - and makes it harder to reason about the
		 * transformation as a whole (instead, bind the source to the target -
		 * i.e. FragmentDocumentNode.TextContainer.setValue)
		 * 
		 * Possibly this isn't really a problem - just make all
		 * FragmentDocumentNode & FragmentDocumentNode.TextContainer be
		 * behavioural decendants of Inline.Anonymous
		 * 
		 * But really the original Inline.Anonymous should be bound to its
		 * source model
		 */
		void insertText() {
			new DepthFirstTraversal<>(root, MeasureNode::getTreeChildren)
					.stream()
					.filter(measureNode -> measureNode.treeChildren.isEmpty())
					.forEach(measureNode -> {
						FragmentDocumentNode node = measureNode.get();
						DomNode createdDomNode = node.domNode();
						MeasureSelection measureSelection = measureNode.measureSelection;
						Measure measure = measureSelection.get();
						DomNode containingNode = measure.startContainingNode();
						if (containingNode.isText()
								&& node instanceof FragmentDocumentNode.TextContainer) {
							String text = measureSelection.get().text();
							if (node instanceof Inline.Text) {
								((Inline.Text) node).setValue(text);
							} else {
								createdDomNode.setText(text);
							}
						}
					});
		}
	}
}