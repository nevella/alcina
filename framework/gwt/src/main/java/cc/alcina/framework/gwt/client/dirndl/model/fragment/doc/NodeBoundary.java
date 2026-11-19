package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

/*
 * Models an node marker (before/after node contents)
 */
@Directed(tag = "nb")
class NodeBoundary extends FragmentNode implements HasMapping {
	@Binding(
		type = Type.PROPERTY,
		to = "contentEditable",
		transform = Binding.DisplayFalseTrueBidi.class)
	boolean contentEditable = false;

	// non-null so there's an initial text node
	@Binding(type = Type.INNER_TEXT)
	String contents = "";

	void setContents(String contents) {
		set("contents", this.contents, contents,
				() -> this.contents = contents);
	}

	// open = 'before' the contents - i.e. 'open tag' as opposed to 'close tag'
	boolean open;

	boolean inline;

	@Override
	public String toString() {
		return Ax.format("boundary:%s - open: %s", tag, open);
	}

	/*
	 * This is a fuzzy concept, but used to indicate (say) that tex should
	 * render as a macro, not an environment. Simple content is either:
	 * 
	 * - inline or
	 * 
	 * - only text/footnotes
	 */
	boolean simpleChildren;

	// inline, but present on a separate line
	boolean separateLine;

	// if previous/subsequent (parent) boundary is a block close, collapse this
	// boundary
	// (boundary
	// style dependent)
	boolean collapseBoundaries;

	// collapse close-close
	boolean collapseBoundaryContainer;

	NodeBoundary previousBoundary;

	FragmentNode fragmentNode;

	String tag;

	NodeBoundary(FragmentNode fragmentNode, boolean before) {
		this.fragmentNode = fragmentNode;
		this.open = before;
		TransformingFragmentModel fragmentModel = (TransformingFragmentModel) fragmentNode
				.fragmentModel();
		fragmentModel.populateMarkerAttributes(this);
		fragmentModel.events.queue(this::populateContents);
	}

	void populateContents() {
		previousBoundary = (NodeBoundary) this.tree().reversed().stream()
				.filter(fn -> fn instanceof NodeBoundary && fn != this)
				.findFirst().orElse(null);
		TransformingFragmentModel fragmentModel = (TransformingFragmentModel) fragmentNode
				.fragmentModel();
		fragmentModel.populateMarkerContent(this);
	}
}