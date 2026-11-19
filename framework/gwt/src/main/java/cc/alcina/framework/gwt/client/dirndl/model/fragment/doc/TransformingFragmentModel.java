package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument.TransformationOptions;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.TransformingFragmentModel.Rule.Position;

/**
 * <p>
 * Customises node transformations, depending on the markup schema etc. The
 * transformations add edit decorators to aid visual editing of an underlying
 * xml model - dashes between catchwrods, element boundary indicators etc.
 * 
 * <p>
 * Note that this approach isn't really geared to incremental changes - for that
 * we'd want an invalidation system (and parents would be invalidated if
 * first/last child changed, siblings if predecessor changed), and a store of
 * existing computed nodes which we'd compare to the newly generated
 * 
 * <p>
 * That's not necessary yet, but would be for large-scale xml/text editor
 * syncing
 */
class TransformingFragmentModel<R extends FragmentDocumentRoot>
		extends FragmentModel {
	TransformationOptions transformationOptions = new TransformationOptions();

	FragmentDocument document;

	TransformingFragmentModel(FragmentDocument document, R root) {
		super(root);
		this.document = document;
		registerPreregisteredNodes(document.domDocument);
	}

	@Override
	public void ensureComputedNodes(FragmentNode fragmentNode) {
		new ComputedNodes(fragmentNode).ensure();
	}

	class ComputedNodes {
		FragmentNode fragmentNode;

		List<? extends FragmentNode> inputNodes;

		List<Rule> rules = new ArrayList<>();

		List<FragmentNode> output;

		boolean hasNoChildBoundaries;

		ComputedNodes(FragmentNode fragmentNode) {
			this.fragmentNode = fragmentNode;
		}

		void ensure() {
			if (fragmentNode instanceof FragmentNode.Leaf) {
				return;
			}
			this.inputNodes = fragmentNode.children()
					.collect(Collectors.toList());
			hasNoChildBoundaries = inputNodes.stream()
					.allMatch(n -> n instanceof FragmentNode.TextLeaf);
			this.output = new ArrayList<>();
			if (transformationOptions.insertBeginEndElementMarkers) {
				rules.add(new Rule(FragmentNode.class, NodeBoundary.class,
						Rule.Position.BEFORE, Rule.PositionFilter.FIRST, null));
				rules.add(new Rule(FragmentNode.class, NodeBoundary.class,
						Rule.Position.AFTER, Rule.PositionFilter.LAST, null));
			}
			// an unusual loop - start at -1, go to inputNodes.size() rather
			// than one less - to handle before/after
			for (int idx = -1; idx <= inputNodes.size(); idx++) {
				for (Rule rule : rules) {
					rule.apply(this, true, idx);
				}
				if (isRealNodeIndex(idx)) {
					FragmentNode node = inputNodes.get(idx);
					output.add(node);
				}
				for (Rule rule : rules) {
					rule.apply(this, false, idx);
				}
			}
			// Insert the created nodes into the correct position (relative to
			// the input order)
			//
			// This separation of input/output (rather than adding per-rule) is
			// primarily for debugging
			if (output.size() != inputNodes.size()) {
				int inputIdx = 0;
				int outputIdx = 0;
				FragmentNode inputTest = null;
				FragmentNode lastOutputNode = null;
				for (; outputIdx < output.size(); outputIdx++) {
					FragmentNode outputNode = output.get(outputIdx);
					if (inputTest == null && inputIdx < inputNodes.size()) {
						inputTest = inputNodes.get(inputIdx++);
					}
					if (outputNode == inputTest) {
						// already a child of the parent. reset inputTest
						inputTest = null;
					} else {
						if (lastOutputNode == null) {
							if (inputTest == null) {
								// empty parent
								fragmentNode.nodes().append(outputNode);
							} else {
								inputTest.nodes().insertBeforeThis(outputNode);
							}
						} else {
							lastOutputNode.nodes().insertAfterThis(outputNode);
						}
					}
					lastOutputNode = outputNode;
				}
			}
		}

		public boolean isRealNodeIndex(int idx) {
			return idx >= 0 && idx < inputNodes.size();
		}

		public void onMatched(Rule rule) {
			Class<? extends FragmentNode> type = rule.generateType;
			FragmentNode node = null;
			if (type == NodeBoundary.class) {
				boolean open = rule.insertPosition == Position.BEFORE;
				node = new NodeBoundary(fragmentNode, open);
			} else {
				node = Reflections.newInstance(type);
			}
			output.add(node);
		}
	}

	static class Rule {
		enum Position {
			BEFORE, AFTER
		}

		enum PositionFilter {
			FIRST {
				@Override
				boolean matches(
						TransformingFragmentModel.ComputedNodes computedNodes,
						int inputNodeIdx, Rule rule) {
					return inputNodeIdx == -1;
				}
			},
			LAST {
				@Override
				boolean matches(
						TransformingFragmentModel.ComputedNodes computedNodes,
						int inputNodeIdx, Rule rule) {
					return inputNodeIdx == computedNodes.inputNodes.size();
				}
			},
			ALL {
				@Override
				boolean matches(
						TransformingFragmentModel.ComputedNodes computedNodes,
						int inputNodeIdx, Rule rule) {
					return computedNodes.isRealNodeIndex(inputNodeIdx);
				}
			},
			EXPR {
				@Override
				boolean matches(
						TransformingFragmentModel.ComputedNodes computedNodes,
						int inputNodeIdx, Rule rule) {
					throw new UnsupportedOperationException();
				}
			};

			abstract boolean matches(
					TransformingFragmentModel.ComputedNodes computedNodes,
					int inputNodeIdx, Rule rule);
		}

		Class<? extends FragmentNode> matchType;

		Position insertPosition;

		PositionFilter filter;

		Class<? extends FragmentNode> generateType;

		Rule(Class<? extends FragmentNode> matchType,
				Class<? extends FragmentNode> generateType,
				Position insertPosition, PositionFilter filter,
				String positionFilterString) {
			this.matchType = matchType;
			this.generateType = generateType;
			this.insertPosition = insertPosition;
			this.filter = filter;
			this.positionFilterString = positionFilterString;
		}

		void apply(TransformingFragmentModel.ComputedNodes computedNodes,
				boolean open, int inputNodeIdx) {
			if (!(open ^ insertPosition == Position.BEFORE)) {
				return;
			}
			boolean match = filter.matches(computedNodes, inputNodeIdx, this);
			if (match) {
				computedNodes.onMatched(this);
			}
		}

		// 2n+1 etc
		String positionFilterString;
	}

	static boolean isInline(FragmentNode fragmentNode) {
		return fragmentNode instanceof Inline
				|| fragmentNode instanceof Block.ListItem.Label;
	}

	static boolean isSimpleChildren(FragmentNode fragmentNode) {
		return fragmentNode instanceof Block.Simple;
	}

	static boolean isCollapseBoundaries(FragmentNode fragmentNode) {
		return fragmentNode instanceof Block.Para;
	}

	static boolean isCollapseBoundaryContainer(FragmentNode fragmentNode) {
		return fragmentNode instanceof Block.ListItem;
	}

	static boolean isSeparateLine(FragmentNode fragmentNode) {
		return fragmentNode instanceof Block.ListItem.Label;
	}

	void populateMarkerAttributes(NodeBoundary boundary) {
		String tag = DirectedRenderer
				.defaultGetTag(boundary.fragmentNode.provideNode(), null);
		boundary.tag = tag;
		boundary.inline = isInline(boundary.fragmentNode);
		boundary.simpleChildren = isSimpleChildren(boundary.fragmentNode);
		boundary.collapseBoundaries = isCollapseBoundaries(
				boundary.fragmentNode);
		boundary.collapseBoundaryContainer = isCollapseBoundaryContainer(
				boundary.fragmentNode);
		boundary.separateLine = isSeparateLine(boundary.fragmentNode);
	}

	void populateMarkerContent(NodeBoundary boundary) {
		boundary.setContents(transformationOptions.computedMarkerType
				.computeMarkerContents(boundary));
	}
}