package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;

// FIXME - selection - slice extends HasSelection? or hasParentSelection?
public class Slice extends Location.Range {
	public static Slice fromNode(DomNode node, LayerToken token) {
		Location.Range range = node.asRange();
		return new Slice(range.start, range.end, token);
	}

	public final LayerToken token;

	/**
	 * Additional match information
	 */
	private Object data;

	private Slice parent;

	private List<Slice> children = new ArrayList<>();

	private Slice aliasedFrom;

	public Slice(Location start, Location end, LayerToken token) {
		super(start, end);
		this.token = token;
	}

	public void addToParent() {
		if (parent != null) {
			parent.children.add(this);
		}
	}

	public Slice alias() {
		Slice alias = new Slice(start, end, token);
		alias.data = data;
		alias.aliasedFrom = this;
		return alias;
	}

	public Selection asSelection(Selection parent) {
		return new SliceSelection(parent, this);
	}

	public Slice childSlice(LayerToken... tokens) {
		return childSlices(tokens).findFirst().orElse(null);
	}

	public Stream<Slice> childSlices(LayerToken... tokens) {
		List<LayerToken> list = Arrays.asList(tokens);
		return children.stream().filter(s -> list.contains(s.token));
	}

	public Object getData() {
		return this.data;
	}

	public Slice getParent() {
		return this.parent;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public Slice subSlice(int start, int end, LayerToken token) {
		Location subStart = this.start.createRelativeLocation(start);
		Location subEnd = this.end.createRelativeLocation(-(length() - end));
		Slice subSlice = new Slice(subStart, subEnd, token);
		subSlice.parent = this;
		return subSlice;
	}

	@Override
	public String toString() {
		String aliasMarker = aliasedFrom != null ? " (alias)" : "";
		return Ax.format("[%s,%s]%s :: %s :: %s", start.index, end.index,
				aliasMarker, token, text());
	}

	public static class SliceSelection extends AbstractSelection<Slice> {
		public SliceSelection(Selection parent, Slice slice) {
			super(parent, slice, slice.toString());
		}

		public SliceSelection(Selection parent, Slice slice,
				String pathSegment) {
			super(parent, slice, pathSegment);
		}
	}
}
