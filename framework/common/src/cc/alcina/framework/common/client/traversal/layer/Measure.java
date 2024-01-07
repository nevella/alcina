package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IdCounter;

// FIXME - selection - Measure extends HasSelection? or hasParentSelection?
/**
 * A Measure models a defined section of a {@link DomDocument} via one or more
 * containing {@Layer} instances, with additional semantic information (token)
 * and structure (children/parent).
 *
 * Measures form a tree structure, and are the model used for traversal and
 * modelling of documents.
 *
 * 
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
	public static final Object NEGATED_MATCH = new Object();

	public static Measure fromNode(DomNode node, Token token) {
		return fromRange(node.asRange(), token);
	}

	public static Measure fromRange(Location.Range range, Token token) {
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
		Preconditions.checkNotNull(token);
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

	/**
	 * if order == null, containment will be true if the measure ranges are
	 * equal ( A contains B and B contains A)
	 */
	public boolean contains(Measure o, Token.Order order, boolean indexOnly) {
		boolean nonEquivalent = false;
		{
			int cmp = start.compareTo(o.start, indexOnly);
			if (cmp > 0) {
				return false;
			}
			nonEquivalent |= cmp < 0;
		}
		{
			int cmp = end.compareTo(o.end, indexOnly);
			if (cmp < 0) {
				// later end (and same start) implies this contains o - so order
				// before
				return false;
			}
			nonEquivalent |= cmp > 0;
		}
		if (nonEquivalent) {
			return true;
		}
		return order == null || order.compare(token, o.token) < 0;
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

	public Measure subMeasure(int start, int end, Token token,
			boolean toTextLocations) {
		Location subStart = this.start.createRelativeLocation(start, false)
				.toTextLocation(toTextLocations);
		Location subEnd = this.end
				.createRelativeLocation(-(length() - end), true)
				.toTextLocation(toTextLocations);
		Measure subMeasure = new Measure(subStart, subEnd, token);
		subMeasure.parent = this;
		return subMeasure;
	}

	@Override
	public String toString() {
		String tokenString = token.toString();
		if (tokenString.contains(token.getClass().getName())) {
			tokenString = token.getClass().getSimpleName();
		}
		return Ax.format("%s-%s :: %s", start.index, end.index, tokenString);
	}

	public String toDebugString() {
		String aliasMarker = aliasedFrom != null ? ":: (alias)" : "";
		String tokenString = token.toString();
		if (tokenString.contains(token.getClass().getName())) {
			tokenString = token.getClass().getSimpleName();
		}
		tokenString = CommonUtils.padStringRight(tokenString, 28, ' ');
		String tokenData = getData() == null ? "" : getData().toString();
		tokenData = CommonUtils.padStringRight(tokenData, 16, ' ');
		return Ax.format("[%s,%s]%s :: %s :: %s :: %s",
				Ax.padLeft(start.index, 8), Ax.padLeft(end.index, 8),
				aliasMarker, tokenString, tokenData, Ax.trimForLogging(text()));
	}

	/**
	 * The type of Measure
	 *
	 * 
	 *
	 */
	public interface Token {
		/*
		 * Parser instruction - parser should traverse node-by-node (rather than
		 * node-boundary-by-node-boundary, a denser traversal) when matching
		 * tokens of this type
		 * 
		 * FIXME - actually node-traversal types are currently:
		 * 
		 * NEXT/PREVIOUS LOCATION (should be NEXT_NON_CONTAINED etc) -
		 * implemented, default (least dense)
		 * 
		 * NEXT_DOMNODE_START/PREVIOUS_DOM_NODE_START - node start by node start
		 * 
		 * NEXT_CONTAINED_LOCATION/PREVIOUS_CONTAINED - boundary by boundary
		 * (densest)
		 */
		public interface NodeTraversalToken extends Token {
		}

		/*
		 * Token will suppress (cause omit from output) any contained tokens
		 */
		public interface NonOutputTree {
		}

		/*
		 * Used in output containment ordering
		 */
		public interface NoPossibleChildren extends Token {
		}

		public interface Order extends Comparator<Token> {
			Order withIgnoreNoPossibleChildren();

			default Order copy() {
				return Reflections.newInstance(getClass());
			}

			public interface Has {
				Order getOrder();
			}

			@Reflected
			public abstract static class Simple implements Order {
				boolean ignoreNoPossibleChildren = false;

				@Override
				public Order withIgnoreNoPossibleChildren() {
					ignoreNoPossibleChildren = true;
					return this;
				}

				@Override
				public int compare(Token o1, Token o2) {
					if (!ignoreNoPossibleChildren) {
						int c1 = noPossibleChildrenWeight(o1);
						int c2 = noPossibleChildrenWeight(o2);
						if (c1 != c2) {
							return c1 - c2;
						}
						// NoPossibleChildren tokens can't overlap
						Preconditions.checkState(c1 == 0);
					}
					return classOrdering(o1.getClass(), o2.getClass());
				}

				private int noPossibleChildrenWeight(Token o) {
					return o instanceof NoPossibleChildren ? 1 : 0;
				}

				protected abstract int classOrdering(
						Class<? extends Token> class1,
						Class<? extends Token> class2);
			}
		}
	}

	static IdCounter counter = new IdCounter(false);

	public String toPathSegment() {
		String tokenString = token.toString();
		if (tokenString.contains(token.getClass().getName())) {
			tokenString = token.getClass().getSimpleName();
		}
		return Ax.format("[%s,%s] %s #%s", Ax.padLeft(start.index, 8),
				Ax.padLeft(end.index, 8), tokenString, counter.nextId());
	}
}
