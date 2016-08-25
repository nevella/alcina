package cc.alcina.framework.entity.parser.token;

import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

import cc.alcina.framework.entity.XmlUtils.SurroundingBlockTuple;

public interface TokenParserPeer<T extends ParserToken, S extends AbstractParserSlice<T>> {
	ParserContext getContext();

	boolean ignoreCitationsInBlock(String content);

	void resetContext(boolean clearMatched);

	boolean processMatch() throws TokenParserException;

	List<T> getTokens();

	S validateMatch(S bestMatch);

	boolean ignoreNode(Node n);

	boolean reverseItalicsForBlocklikeAround(Text txt,
			SurroundingBlockTuple surroundingTuple);

	void flushRunContextAndCatch(boolean end);

	boolean isEmphasised(Text t);

	boolean isBold(Node n);

	boolean isMultiLine();

	boolean continueBlock(SurroundingBlockTuple lastSurroundingTuple,
			SurroundingBlockTuple surroundingTuple);
}
