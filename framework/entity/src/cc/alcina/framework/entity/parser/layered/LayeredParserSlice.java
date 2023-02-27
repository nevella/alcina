package cc.alcina.framework.entity.parser.layered;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.util.Ax;

public class LayeredParserSlice extends Location.Range {
	public final LayeredParserToken token;

	/**
	 * Additional match information
	 */
	private Object data;

	private LayeredParserSlice parent;

	private List<LayeredParserSlice> children = new ArrayList<>();

	public LayeredParserSlice(Location start, Location end,
			LayeredParserToken token) {
		super(start, end);
		this.token = token;
	}

	public Object getData() {
		return this.data;
	}

	public LayeredParserSlice getParent() {
		return this.parent;
	}

	public void removeFromParent() {
		parent.children.remove(this);
		parent = null;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public LayeredParserSlice subSlice(int start, int end,
			LayeredParserToken token) {
		Location subStart = this.start.createRelativeLocation(start);
		Location subEnd = this.end.createRelativeLocation(-(length() - end));
		LayeredParserSlice subSlice = new LayeredParserSlice(subStart, subEnd,
				token);
		subSlice.parent = this;
		parent.children.add(subSlice);
		return subSlice;
	}

	@Override
	public String toString() {
		return Ax.format("[%s,%s] :: %s :: %s", start.index, end.index, token,
				textContent());
	}
}
