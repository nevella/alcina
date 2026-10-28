package cc.alcina.framework.servlet.transform.fragment.output;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block.IndentStyles;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FootnoteNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FootnoteNode.FootnoteReferenceTarget;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Footnotes;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentRoot;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline.Pagebreak;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Pagination;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Pagination.Page;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Pagination.PageBreakBlock;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.RootToken.HasFragmentDocumentRoot;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformer;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerPeer;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;

/*
@formatter:off

## Steps

- paginate the content by inserting pagebreak FragmentDocumentNode instances (note that existing pagebreaks - from say 
  a Word doc - are respected)
- traverse the content and create Page objects 


Then:

- Fix WrapCoverImage (remove pgbrk dep? )

@formatter:on
 */
public class OutputPagination2 extends Layer<DocumentTransformerSelection> {
	static Configuration.Key validateSequence = Configuration
			.key("validateSequence");

	static Configuration.Key enabled = Configuration.key("enabled");

	@Override
	public void process(DocumentTransformerSelection selection)
			throws Exception {
		if (!enabled.is()) {
			return;
		}
		HasFragmentDocumentRoot hasFragmentDocumentRoot = state
				.traversalContext(HasFragmentDocumentRoot.class);
		root = hasFragmentDocumentRoot.provideRoot();
		IndentStyles indentStyles = root.byTypeNode(Block.IndentStyles.class);
		new PageBreaks().insert();
		new CreatePages().run();
		Footnotes footnotes = new Footnotes(root.document);
		footnotes.buildRefTargetByNumberMap();
		// must be before ensureContentsParentStructure
		if (!DocumentTransformer.isPartialDocumentTransform()) {
			pages.forEach(page -> page.moveFootnotes(footnotes));
		}
		boolean pdf2html = state.traversalContext(DocumentTransformerPeer.class)
				.isPdf2html();
		if (pdf2html) {
			pages.removeIf(page -> page.hasNoContentNodes());
		}
		pages.forEach(Page::ensureContentsParentStructure);
		pages.forEach(page -> page.setStylesToReplicate(indentStyles));
		pages.forEach(page -> page.appendTo(root));
		pages.forEach(Page::removePagebreakNode);
		pages.forEach(Page::ensureSplitNodeStructure);
		pages.stream().filter(Page::hasEmptyContent)
				.forEach(Page::removeFromFlow);
		if (validateSequence.is()) {
			new PageBreaks().validateSequence();
		}
		new PageBreaks().renumberBreaks();
	}

	public static boolean isPageBreak(DomNode node) {
		return node.isProcessingInstruction() && node.tagIs("pgbrk")
				|| node.tagAndClassIs("block", "pgbrk");
	}

	class CreatePages {
		Page current;

		public void run() {
			root.stream().forEach(node -> {
				if (node == root) {
					return;
				}
				if (node.isDetached()) {
					return;
				}
				// the first non-root node will be a pagebreak, ensuring current
				// is non-null for subsequent appends
				if (node instanceof Inline.Pagebreak) {
					current = new Page((Pagebreak) node);
					pages.add(current);
				} else {
					if (node.ancestors().has(FootnoteReferenceTarget.class)) {
					} else {
						current.addNode((FragmentDocumentNode) node);
					}
				}
			});
		}
	}

	FragmentDocumentRoot root;

	List<Page> pages = new ArrayList<>();

	/*
			@formatter:off
			 * This class inserts pagebreak Nodes in a block document
			 * 
			 * Wrinkles are:
			 * - don't break in tables. *Do* break in list items [splitting code will have to handle with empty li/label tree]
			 * - footnote refs: count the footnote _target_ chars
			 * - break at block boundaries
			 * 
			 * @formatter:on
			 */
	class PageBreaks {
		Map<String, String> footnoteTexts = new LinkedHashMap<>();

		void insert() {
			root.tree().stream().forEach(this::buildFootnoteTargets);
			// insert a pagebreak as the first child of the doc root
			Inline.Pagebreak firstPagebreak = new Inline.Pagebreak();
			root.nodes().insertAsFirstChild(firstPagebreak);
			setPagebreakTitle(firstPagebreak);
			root.tree().stream()
					// the usual 'avoid concurrent modification' construction
					.toList().forEach(this::accept);
		}

		/*
		 * remove a trailing pagebreak if any (cleaning of blank pages should
		 * only affect the last, and that if the doc ends in a table)
		 * 
		 * then verify that the top-level structure is {pagebreak, page}+
		 */
		void validateSequence() {
			List<? extends FragmentNode> rootChildren = root.children()
					.collect(Collectors.toList());
			FragmentNode last = Ax.last(rootChildren);
			if (last instanceof Pagination.PageBreakBlock) {
				last.nodes().removeFromParent();
				rootChildren.remove(last);
			}
			for (int idx = 0; idx < rootChildren.size(); idx += 2) {
				FragmentNode child = rootChildren.get(idx);
				Preconditions
						.checkState(child instanceof Pagination.PageBreakBlock);
				FragmentNode child1 = rootChildren.get(idx + 1);
				Preconditions.checkState(child1 instanceof Pagination.Page);
			}
		}

		// because pgbrks may have been removed, renumber
		void renumberBreaks() {
			int pageNumber = 1;
			List<PageBreakBlock> breaks = root
					.byType(Pagination.PageBreakBlock.class).toList();
			for (PageBreakBlock block : breaks) {
				block.putNumber(pageNumber++);
			}
		}

		void buildFootnoteTargets(FragmentNode node) {
			if (node instanceof FootnoteNode.FootnoteReferenceTarget) {
				FootnoteNode.FootnoteReferenceTarget target = (FootnoteNode.FootnoteReferenceTarget) node;
				footnoteTexts.put(target.getNumber(),
						target.provideNode().getRendered().asDomNode().ntc());
			}
		}

		int charsCounter = 0;

		int pageCounter = 1;

		boolean lastWasSection;

		boolean inFootnotesSection;

		static final int ONE_LINE_CHARS = 60;

		static final int ONE_PAGE_CHARS = 2500;

		StringBuilder buffer = new StringBuilder();

		void accept(FragmentNode node) {
			boolean inFootnoteTarget = node.ancestors()
					.has(FootnoteNode.FootnoteReferenceTarget.class);
			if (inFootnoteTarget) {
				// counts will come from footnoteCharCounts, and won't break til
				// the end of the containing block
				return;
			}
			/*
			 * Modify the char count
			 */
			if (node instanceof Inline.Text) {
				if (node.ancestor(Block.IndentStyles.class).isPresent()) {
					return;
				}
				String normalisedText = ((Inline.Text) node).domNode().ntc();
				buffer.append(normalisedText);
				buffer.append("\n");
				charsCounter += normalisedText.length();
			} else if (node instanceof FootnoteNode.FootnoteReference) {
				if (!DocumentTransformer.isPartialDocumentTransform()) {
					String number = ((FootnoteNode.FootnoteReference) node)
							.getNumber();
					String footnoteText = footnoteTexts.get(number);
					if (footnoteText.length() < ONE_LINE_CHARS) {
						buffer.append("[one line footnote]");
						charsCounter += ONE_LINE_CHARS;
					} else {
						buffer.append(footnoteText);
						charsCounter += footnoteText.length();
					}
					buffer.append("\n");
				}
			}
			/*
			 * Break if at the start of a block and charCount is over
			 * 
			 * Except :: if in a table; in the body of a list item
			 */
			boolean inTable = node.ancestors().has(Block.TableNode.Table.class);
			boolean inListItem = inDirectListItem(node);
			if (inTable || inListItem) {
				return;
			}
			Inline.Pagebreak pagebreak = null;
			if (node instanceof Block) {
				if (charsCounter > ONE_PAGE_CHARS) {
					pagebreak = new Inline.Pagebreak();
					node.nodes().insertBeforeThis(pagebreak);
					charsCounter = 0;
					buffer = new StringBuilder();
				}
			} else if (node instanceof Inline.Pagebreak) {
				pagebreak = (Inline.Pagebreak) node;
			}
			if (pagebreak != null) {
				setPagebreakTitle(pagebreak);
			}
		}

		/*
		 * return true for list/listitem//node - but not for (say)
		 * listitem//list/node or list/node
		 */
		boolean inDirectListItem(FragmentNode node) {
			for (FragmentNode ancestor : node.ancestors().excludeSelf().stream()
					.toList()) {
				if (ancestor.getClass() == Block.ListItem.class) {
					return true;
				}
				if (ancestor.getClass() == Block.UnorderedList.class) {
					return false;
				}
				if (ancestor.getClass() == Block.OrderedList.class) {
					return false;
				}
			}
			return false;
		}

		void setPagebreakTitle(Inline.Pagebreak pagebreak) {
			charsCounter = 0;
			buffer = new StringBuilder();
			String pageNumberString = Ax.format("Page %s", pageCounter++);
			pagebreak.setStringValue(pageNumberString);
		}
	}
}