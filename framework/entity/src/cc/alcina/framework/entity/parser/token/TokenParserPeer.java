package cc.alcina.framework.entity.parser.token;

import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

import cc.alcina.framework.entity.XmlUtils.SurroundingBlockTuple;

public interface TokenParserPeer<T extends ParserToken, S extends AbstractParserSlice<T>> {
	default void beforeMatchTokens() {
	}

	boolean continueBlock(SurroundingBlockTuple lastSurroundingTuple,
			SurroundingBlockTuple surroundingTuple);

	void flushRunContextAndCatch(boolean end);

	ParserContext getContext();

	default SurroundingBlockTuple getSurroundingBlockTuple(Node n) {
		return null;
	}

	List<T> getTokens();

	boolean ignoreCitationsInBlock(String content);

	boolean ignoreNode(Node n);

	default boolean ignorePrecedingExceptions() {
		return false;
	}

	boolean isBold(Node n);

	boolean isEmphasised(Text t);

	boolean isMultiLine();

	boolean processMatch() throws TokenParserException;

	void resetContext(boolean clearMatched);

	boolean reverseItalicsForBlocklikeAround(Text txt,
			SurroundingBlockTuple surroundingTuple);

	S validateMatch(S bestMatch);
}
