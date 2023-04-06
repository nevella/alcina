package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.InputState;
import cc.alcina.framework.common.client.util.Ax;

// FIXME - selection - Measure extends HasSelection? or hasParentSelection?
/**
 * A Measure models a defined section of a {@link DomDocument} via one or more
 * containing {@Layer} instances, with additional semantic information (token)
 * and structure (children/parent).
 *
 * Measures form a tree structure, and are the model used for traversal and
 * modelling of documents.
 *
 * @author nick@alcina.cc
 *
 */
/*
 * The name isn't really that cute (although it's a nod to both music and
 * measure theory) - the underlying location/range model really is strangely
 * reminiscent of a lebesgue measure, in that it contains one "flat" dimension
 * (text run) and one potentially unbounded set of 'holes' (tree
 * indicies/positions, which have zero width in the text run dimension)
 *
 * Anyways, I was tired of slice.
 */
public class Measure extends Location.Range {
	public static Measure fromNode(DomNode node, Token token) {
		Location.Range range = node.asRange();
		return new Measure(range.start, range.end, token);
	}

	public final Token token;

	/**
	 * Additional match information
	 */
	private Object data;

	private Measure parent;

	private List<Measure> children = new ArrayList<>();

	private Measure aliasedFrom;

	public Measure(Location start, Location end, Token token) {
		super(start, end);
		this.token = token;
	}

	public void addToParent() {
		if (parent != null) {
			parent.children.add(this);
		}
	}

	public Measure alias() {
		Measure alias = new Measure(start, end, token);
		alias.data = data;
		alias.aliasedFrom = this;
		return alias;
	}

	public Selection asSelection(Selection parent) {
		return new MeasureSelection(parent, this);
	}

	public Measure childMeasure(Token... tokens) {
		return childMeasures(tokens).findFirst().orElse(null);
	}

	public Stream<Measure> childMeasures(Token... tokens) {
		List<Token> list = Arrays.asList(tokens);
		return children.stream().filter(m -> list.contains(m.token));
	}

	public Object getData() {
		return this.data;
	}

	public Measure getParent() {
		return this.parent;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public Measure subMeasure(int start, int end, Token token) {
		Location subStart = this.start.createRelativeLocation(start, false);
		Location subEnd = this.end.createRelativeLocation(-(length() - end),
				true);
		Measure subMeasure = new Measure(subStart, subEnd, token);
		subMeasure.parent = this;
		return subMeasure;
	}

	@Override
	public String toString() {
		String aliasMarker = aliasedFrom != null ? " (alias)" : "";
		return Ax.format("[%s,%s]%s :: %s :: %s", start.index, end.index,
				aliasMarker, token, text());
	}

	public static class MeasureSelection extends AbstractSelection<Measure> {
		public MeasureSelection(Selection parent, Measure measure) {
			super(parent, measure, measure.toString());
		}

		public MeasureSelection(Selection parent, Measure measure,
				String pathSegment) {
			super(parent, measure, pathSegment);
		}
	}

	public interface Token {
		Measure match(InputState state);

		Selection select(InputState state, Measure measure);

		public static abstract class SingleMatch implements Token {
			private Optional<DomNode> match;

			@Override
			public Measure match(InputState state) {
				if (match == null) {
					DomNode document = state.getDocument().get()
							.containingNode();
					this.match = getMatch(document);
				}
				if (match.isPresent()
						&& state.contains(match.get().asLocation())) {
					return Measure.fromNode(match.get(), this);
				} else {
					return null;
				}
			}

			protected abstract Optional<DomNode> getMatch(DomNode document);
		}
	}
}
