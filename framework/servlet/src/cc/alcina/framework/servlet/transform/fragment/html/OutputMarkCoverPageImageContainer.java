package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block.Para;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Pagination;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Pagination.Page;
import cc.alcina.framework.servlet.transform.fragment.OutputMutationSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;

/**
 * <p>
 * Mark a coverpage image container
 * 
 */
class OutputMarkCoverPageImageContainer extends Layer<MeasureNode> {
	@Override
	public void process(MeasureNode selection) throws Exception {
		if (coverPageMarked) {
			return;
		}
		if (selection.get() instanceof Para) {
			Para para = (Para) selection.get();
			if (!para.provideIsBound()) {
				return;
			}
			List<? extends FragmentNode> children = para.children()
					.filter(Inline.Text.isWhitespaceOrEmpty().negate())
					.collect(Collectors.toList());
			if (children.stream().allMatch(this::isImage)
					&& children.size() == 1) {
				Pagination.Page page = para.ancestor(Pagination.Page.class)
						.get();
				if (page.isFirstPage()) {
					markPara(selection, para);
				}
			}
		} else if (selection.get() instanceof Inline.Image) {
			Inline.Image image = (Inline.Image) selection.get();
			if (!image.provideIsBound()) {
				return;
			}
			if (image.parent().getClass() == Pagination.Page.class) {
				Pagination.Page page = (Page) image.parent();
				if (page.isFirstPage()) {
					Block.Para para = new Block.Para();
					image.nodes().wrapWith(para);
					select(new WrappingParaCreated(selection, para));
					markPara(selection, para);
				}
			}
		}
	}

	private void markPara(MeasureNode selection, Para para) {
		Para.properties.coverPageContainer.set(para, true);
		select(new CoverPageMarked(selection, para));
		coverPageMarked = true;
	}

	boolean coverPageMarked = false;

	boolean isImage(FragmentNode node) {
		return node instanceof Inline.Image;
	}

	/**
	 * Records that a Para was marked as coverPageContainer
	 */
	static class CoverPageMarked extends OutputMutationSelection {
		public CoverPageMarked(MeasureNode parent, FragmentDocumentNode value) {
			super(parent, value);
			parent.set(value);
		}
	}

	static class WrappingParaCreated extends OutputMutationSelection {
		public WrappingParaCreated(MeasureNode parent,
				FragmentDocumentNode value) {
			super(parent, value);
			parent.set(value);
		}
	}
}