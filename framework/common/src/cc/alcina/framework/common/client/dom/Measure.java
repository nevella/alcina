package cc.alcina.framework.common.client.dom;

import java.util.Comparator;
import java.util.Objects;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FileLogger;
import cc.alcina.framework.common.client.util.IdCounter;
import cc.alcina.framework.common.client.util.NestedName;

// FIXME - selection - Measure extends HasSelection? or hasParentSelection?
/**
 * A Measure models a defined section of a {@link DomDocument} classified with
 * additional semantic information ({@link Measure.Token}) and an aribtrary
 * {@link #data} object.
 *
 * 
 * Measures often have a containment relationship - and thus a tree structure,
 * and are the model used for traversal and modelling of documents.
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

	static IdCounter counter = new IdCounter(false);

	public static Measure fromNode(DomNode node, Token token) {
		return fromRange(node.asRange(), token);
	}

	public static Measure fromRange(Location.Range range, Token token) {
		return new Measure(range.start, range.end, token);
	}

	public Measure withData(Object data) {
		this.data = data;
		return this;
	}

	public final Token token;

	/**
	 * Additional match information
	 */
	private Object data;

	private Measure aliasedFrom;

	public Location.Range toRange() {
		return new Location.Range(start, end);
	}

	public Measure(Location start, Location end, Token token) {
		super(start, end);
		Preconditions.checkNotNull(token);
		this.token = token;
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

	public boolean hasDataAssignableClass(Class<?> dataClass) {
		return data != null
				&& Reflections.isAssignableFrom(dataClass, data.getClass());
	}

	/**
	 * if order == null, containment will be true if the measure ranges are
	 * equal ( A contains B and B contains A)
	 */
	public boolean contains(Measure o, Token.Order order) {
		boolean nonEquivalent = false;
		{
			int cmp = start.compareTo(o.start, false);
			if (cmp > 0) {
				return false;
			}
			nonEquivalent |= cmp < 0;
		}
		{
			int cmp = end.compareTo(o.end, false);
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
		// this allows measures of the same token type to contain each other
		return order.compare(token, o.token) <= 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Measure) {
			Measure o = (Measure) obj;
			return Ax.equals(start, o.start, end, o.end, token, o.token, data,
					o.data, aliasedFrom, o.aliasedFrom);
		} else {
			return super.equals(obj);
		}
	}

	public Object getData() {
		return this.data;
	}

	public Token getToken() {
		return token;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), token.hashCode(), data,
				aliasedFrom);
	}

	public String log() {
		String markup = markup();
		String result = Ax.format("%s :: %s", toIntPair(),
				Ax.trimForLogging(markup));
		Ax.out(result);
		FileLogger.log(markup);
		return result;
	}

	public void setData(Object data) {
		this.data = data;
	}

	/*
	 * start and end are _text_ (index) offsets relative to the start of this
	 * measure
	 */
	public Measure subMeasure(int startOffset, int endOffset, Token token,
			boolean toTextLocations) {
		Location subStart = this.start.textRelativeLocation(startOffset, false)
				.toTextLocation(toTextLocations).toStartTextLocationIfAtEnd();
		Location subEnd = endOffset == length()
				? this.end.toTextLocation(toTextLocations)
				: this.start.textRelativeLocation(endOffset, false);
		if (startOffset != endOffset) {
			subEnd = subEnd.toTextLocation(toTextLocations)
					.toEndTextLocationIfAtStart();
		}
		Measure subMeasure = new Measure(subStart, subEnd, token);
		return subMeasure;
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
				Ax.padLeft(start.getIndex(), 8), Ax.padLeft(end.getIndex(), 8),
				aliasMarker, tokenString, tokenData, Ax.trimForLogging(text()));
	}

	public String toPathSegment() {
		String tokenString = token.toString();
		if (tokenString.contains(token.getClass().getName())) {
			tokenString = token.getClass().getSimpleName();
		}
		return Ax.format("[%s,%s] %s #%s", Ax.padLeft(start.getIndex(), 8),
				Ax.padLeft(end.getIndex(), 8), tokenString, counter.nextId());
	}

	@Override
	public String toString() {
		String tokenString = token.toString();
		if (tokenString.contains(token.getClass().getName())) {
			tokenString = token.getClass().getSimpleName();
		}
		if (start.getTreeIndex() == end.getTreeIndex()
				&& start.getContainingNode() != null
				&& start.getContainingNode().isText()) {
			return Ax.format("%s-%s :: %s", start.getIndex(), end.getIndex(),
					tokenString);
		} else {
			return Ax.format("[%s =>  %s] :: %s", start.toLocationString(),
					end.toLocationString(), tokenString);
		}
	}

	public String toTextString() {
		return Ax.format("%s :: %s", toIntPair(), Ax.trimForLogging(text()));
	}

	public String toTokenRangeTextString() {
		String tokenString = token.toString();
		if (tokenString.contains(token.getClass().getName())) {
			tokenString = token.getClass().getSimpleName();
		}
		return Ax.format("%s :: %s :: %s", tokenString, toIntPair(),
				Ax.trimForLogging(text()));
	}

	public <T> T typedData() {
		return (T) getData();
	}

	/**
	 * The type of Measure
	 *
	 * 
	 *
	 */
	public interface Token {
		/*
		 * Logical boundary tokens are non-dom - when matched, they do not move
		 * the match cursor forward
		 */
		default boolean isNonDomToken() {
			return false;
		}

		/**
		 * This will be overridden if the token is an enum
		 * 
		 * @return the name
		 */
		default String name() {
			return NestedName.get(this);
		}

		/*
		 * Containment is generally based on a linear ordering. But tokens which
		 * can correspond to measures with recursive containment should
		 * implement this interface and allow (or disallow) appropriate
		 * containments
		 */
		public interface CheckDescendantContainment extends Token {
			boolean allowDescendantContainment(Token otherToken);

			default boolean allowDescendantContainment(Measure measure) {
				return allowDescendantContainment(measure.token);
			}
		}

		/*
		 * This class handles containments which would invalidate DOM
		 */
		public interface RestrictedChildContainment extends Token {
			boolean allowChildContainment(Token otherToken);
		}

		/*
		 * For transport measures where the token is unused
		 */
		public static class Generic implements Measure.Token {
			public static final Generic TYPE = new Generic();

			Generic() {
			}
		}

		public static class DocumentElementToken implements Measure.Token {
			public static final DocumentElementToken TYPE = new DocumentElementToken();

			DocumentElementToken() {
			}
		}

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
			default Order copy() {
				return Reflections.newInstance(getClass());
			}

			public interface Has {
				Order getOrder();
			}

			@Reflected
			public abstract static class Simple implements Order {
				protected abstract int classOrdering(
						Class<? extends Token> class1,
						Class<? extends Token> class2);

				@Override
				public int compare(Token o1, Token o2) {
					return classOrdering(CommonUtils.getComparableType(o1),
							CommonUtils.getComparableType(o2));
				}
			}
		}

		public interface IgnoreEmptyText {
		}
	}

	public Measure trimWhitespace() {
		Measure result = this;
		Measure lastResult = null;
		while (result != lastResult) {
			lastResult = result;
			String text = result.text();
			if (text.startsWith(" ")) {
				result = result.truncateMeasureToIndexStart(1);
			} else if (text.endsWith(" ")) {
				result = result.truncateMeasureToIndexEnd(text.length() - 1);
			}
		}
		return result;
	}

	public Measure truncateMeasureToIndexStart(int startIndex) {
		return Measure.fromRange(truncateToIndexStart(startIndex), token);
	}

	/**
	 * 
	 * @param endIndex
	 *            (endIndex==measure.lengt() returns the whole range,
	 *            endIndex==0 returns an empty measure at start)
	 * @return
	 */
	public Measure truncateMeasureToIndexEnd(int endIndex) {
		return Measure.fromRange(truncateToIndexEnd(endIndex), token);
	}
}
