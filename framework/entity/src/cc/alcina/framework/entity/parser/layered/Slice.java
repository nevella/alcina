package cc.alcina.framework.entity.parser.layered;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.util.Ax;

public class Slice extends Location.Range {
	public final Token token;

	/**
	 * Additional match information
	 */
	private Object data;

	private Slice parent;

	private List<Slice> children = new ArrayList<>();

	private Slice aliasedFrom;

	public Slice(Location start, Location end, Token token) {
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

	public Slice childSlice(Token... tokens) {
		return childSlices(tokens).findFirst().orElse(null);
	}

	public Stream<Slice> childSlices(Token... tokens) {
		List<Token> list = Arrays.asList(tokens);
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

	public Slice subSlice(int start, int end, Token token) {
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
}
