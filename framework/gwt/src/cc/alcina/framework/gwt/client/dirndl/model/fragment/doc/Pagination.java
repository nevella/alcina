package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CloneHelper;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block.ListItem.Label;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block.ListItem.ListItemContents;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FootnoteNode.FootnoteReference;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FootnoteNode.FootnoteReferenceTarget;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FootnoteNode.FootnotesContainer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode.Mapping;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline.Pagebreak;

public interface Pagination {
	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block", className = "page"))
	public static class Page extends Block {
		public Page() {
		}

		public Page(Pagebreak pagebreak) {
			this.pageBreak = pagebreak;
		}

		@Property.Not
		public boolean isFirstPage() {
			/*
			 * this may change after page creation due to pagebreak renumbering
			 */
			if (provideIsBound()) {
				FragmentNode previousSibling = nodes().previousSibling();
				if (previousSibling instanceof Pagination.PageBreakBlock) {
					return ((Pagination.PageBreakBlock) previousSibling).firstPage;
				}
			}
			return pageBreak.provideIsFirstPage();
		}

		Inline.Pagebreak pageBreak;

		List<FragmentDocumentNode> contentNodes = new ArrayList<>();

		FootnotesContainer footnotesContainer = new FootnotesContainer();

		PageBreakBlock generatePagebreakBlock() {
			return new PageBreakBlock(pageBreak.getStringValue(),
					pageBreak.provideIsFirstPage());
		}

		@Override
		public void onFragmentRegistration() {
			contentNodes.forEach(nodes()::append);
			/*
			 * Attach in (tree) descending order
			 */
			while (replicatedContentNodeContents.size() > 0) {
				Iterator<Entry<FragmentDocumentNode, List<FragmentDocumentNode>>> itr = replicatedContentNodeContents
						.entrySet().iterator();
				while (itr.hasNext()) {
					Entry<FragmentDocumentNode, List<FragmentDocumentNode>> entry = itr
							.next();
					FragmentDocumentNode node = entry.getKey();
					if (node.provideIsBound()) {
						entry.getValue().forEach(node.nodes()::append);
						itr.remove();
					}
				}
			}
			if (footnotesContainer.footnoteReferenceTargets.size() > 0) {
				nodes().append(footnotesContainer);
			}
		}

		Multimap<FragmentDocumentNode, List<FragmentDocumentNode>> replicatedContentNodeContents;

		public void removePagebreakNode() {
			if (pageBreak != null) {
				pageBreak.removeFromParent();
			}
		}

		public boolean hasEmptyContent() {
			List<? extends FragmentNode> list = stream()
					.filter(n -> n.nodes().isLeaf())
					.filter(fn -> fn.domNode().isNonWhitespaceTextContent()
							|| fn instanceof Inline.Image)
					.filter(fn -> !fn.ancestors().has(Block.IndentStyles.class))
					.toList();
			return list.isEmpty();
		}

		public void removeFromFlow() {
			FragmentNode nextSibling = nodes().nextSibling();
			FragmentNode previousSibling = nodes().previousSibling();
			if (nextSibling instanceof PageBreakBlock
					&& previousSibling instanceof PageBreakBlock) {
				previousSibling.removeFromParent();
			}
			removeFromParent();
		}

		public void ensureContentsParentStructure() {
			Preconditions.checkState(contentNodes.size() > 0);
			Map<FragmentDocumentNode, FragmentDocumentNode> replicatedContentNodes = new LinkedHashMap<>();
			/*
			 * We can only append to attached FragmentNodes - currently - so use
			 * this structure to buffer appends. The node movement won't happen
			 * until the page is attached to the dom
			 */
			replicatedContentNodeContents = new Multimap<>();
			FragmentDocumentRoot root = (FragmentDocumentRoot) contentNodes
					.get(0).fragmentModel().getFragmentRoot().getRootModel();
			List<FragmentDocumentNode> toReparent = contentNodes.stream()
					.filter(n -> n.parent() != root).toList();
			if (toReparent.isEmpty()) {
				return;
			}
			toReparent.forEach(n -> {
				/*
				 * in the first loop iteration, the cursor child (to append to
				 * the split structure) will be from the old page, subsequently
				 * it will be the replicated nodes (going up the tree)
				 */
				FragmentDocumentNode cursorChild = n;
				FragmentDocumentNode cursor = (FragmentDocumentNode) cursorChild
						.parent();
				while (cursor != root) {
					/*
					 * logically, the toReparent node n is a split child (its
					 * ancestors up to root are initially in the previous page)
					 */
					FragmentDocumentNode replicated = replicatedContentNodes
							.get(cursor);
					if (replicated == null) {
						replicated = CloneHelper.fieldwiseClone(cursor);
						replicatedContentNodes.put(cursor, replicated);
						replicated.splitReplicatedFrom = cursor;
						replicated.setSplit(true);
						replicatedContentNodeContents.add(replicated,
								cursorChild);
						cursorChild = // cursor;
								replicated;
						cursor = (FragmentDocumentNode) cursor.parent();
					} else {
						replicatedContentNodeContents.add(replicated,
								cursorChild);
						break;
					}
				}
			});
			/*
			 * remove the nodes that will be reparented
			 */
			contentNodes.removeIf(toReparent::contains);
			Set<FragmentDocumentNode> replicantChildren = replicatedContentNodeContents
					.allValues().stream()
					.collect(AlcinaCollectors.toLinkedHashSet());
			Set<FragmentDocumentNode> topLevel = replicatedContentNodeContents
					.keySet().stream()
					.filter(n -> !replicantChildren.contains(n))
					.collect(Collectors.toSet());
			/*
			 * logically, only one node tree (child of root) will be split - the
			 * replicated root of that tree will be the single element of
			 * topLevel
			 */
			Preconditions.checkState(topLevel.size() == 1);
			contentNodes.add(0, topLevel.iterator().next());
		}

		public void ensureSplitNodeStructure() {
			byType(Block.ListItem.class).filter(n -> n.split).forEach(n -> {
				FragmentNode firstChild = n.nodes().firstChild();
				if (firstChild instanceof ListItemContents) {
					Label label = new ListItem.Label("\u00a0");
					n.nodes().insertAsFirstChild(label);
				}
			});
		}

		public void addNode(FragmentDocumentNode node) {
			if (contentNodes.stream().anyMatch(node.ancestors()::contains)) {
				//
			} else {
				contentNodes.add(node);
			}
		}

		public void moveFootnotes(Footnotes footnotes) {
			contentNodes.stream().flatMap(FragmentDocumentNode::stream)
					.forEach(n -> {
						if (n instanceof FootnoteReference) {
							FootnoteReference ref = (FootnoteReference) n;
							FootnoteReferenceTarget target = footnotes
									.getReferenceTarget(ref);
							Objects.requireNonNull(target);
							footnotesContainer.footnoteReferenceTargets
									.add(target);
						}
					});
		}

		public void appendTo(FragmentDocumentRoot root) {
			root.nodes().append(generatePagebreakBlock());
			root.nodes().append(this);
		}

		public void setStylesToReplicate(IndentStyles indentStyles) {
			if (indentStyles != null && !contentNodes.contains(indentStyles)) {
				// TODO - this should be tracked by a
				// PaginationReplicationSelection
				contentNodes.add(0, indentStyles.copy());
			}
		}

		public boolean hasNoContentNodes() {
			return contentNodes.isEmpty();
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block", className = "pgbrk"))
	@TypedProperties
	public static class PageBreakBlock extends Block {
		PackageProperties._Pagination_PageBreakBlock.InstanceProperties
				properties() {
			return PackageProperties.pagination_pageBreakBlock.instance(this);
		}

		@Binding(type = Type.PROPERTY)
		public String text;

		@Binding(type = Type.PROPERTY)
		public boolean firstPage;

		public PageBreakBlock() {
		}

		@Override
		public void onFragmentRegistration() {
			nodes().append(new PageBreakNumber(text));
		}

		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(
				tag = "block",
				className = "page-break-number"))
		@TypedProperties
		public static class PageBreakNumber extends Block {
			PackageProperties._Pagination_PageBreakBlock_PageBreakNumber.InstanceProperties
					properties() {
				return PackageProperties.pagination_pageBreakBlock_pageBreakNumber
						.instance(this);
			}

			public PageBreakNumber() {
			}

			public PageBreakNumber(String text) {
				this.text = text;
			}

			// @Binding(type = Type.INNER_TEXT)
			/*
			 * A property means this can be a css pseudo-selector content, and
			 * not in the search text -
			 * https://barnet.atlassian.net/browse/CCJCV-446
			 */
			@Binding(type = Type.PROPERTY)
			public String text;
		}

		PageBreakBlock(String text, boolean firstPage) {
			this.text = text;
			this.firstPage = firstPage;
		}

		public void putNumber(int pageNumber) {
			String newText = Ax.format("Page %s", pageNumber);
			properties().text().set(newText);
			properties().firstPage().set(pageNumber == 1);
			byTypeOptional(PageBreakNumber.class)
					.ifPresent(pbn -> pbn.properties().text().set(newText));
		}
	}
}