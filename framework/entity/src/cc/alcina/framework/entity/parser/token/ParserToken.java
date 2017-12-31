package cc.alcina.framework.entity.parser.token;

import java.util.regex.Pattern;

import org.w3c.dom.Node;

import cc.alcina.framework.entity.XmlUtils;

public interface ParserToken<C extends ParserContext, S extends AbstractParserSlice> {
	public static final String IGNORE_STATUTE_MATCH_PREFIXED_BY = "in the matter of the ";

	public S createSlice(C context, XmlUtils.DOMLocation start,
			XmlUtils.DOMLocation end, int startOffsetInRun);

	public S createSlice(Node node);

	public S extractSubstringAndMatch(C context);

	public Class getCategory();

	public abstract Pattern getPattern(C context);

	public boolean isIgnoreable(C context);

	public boolean isStopToken(C context);

	public S match(C context, String visibleSubstring);

	public MatchesEmphasisTypes matchesEmphasisTypes();

	public S matchWithFollowCheck(C context);

	public void onMatch(C context, S slice);

	public abstract boolean overridesAtSameLocation(S slice);

	public boolean shouldStartNewSequence(C context);

	public boolean skipMatchingWhitespace(C context, String stringToMatch,
			int start);

	boolean canFollow(C context);

	S currentRangeAsSliceAndIncrementOffset(C context, int trimFromEnd);

	boolean isGreedy(C context, S bestMatch);
}