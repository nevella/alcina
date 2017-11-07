package cc.alcina.framework.entity.parser.token;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.XmlUtils.SurroundingBlockTuple;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;

public class TokenParser<T extends ParserToken, S extends AbstractParserSlice<T>> {
	public static String debugMarker = "%%^^##**x";

	private TokenParserPeer<T, S> peer;

	private boolean trace;

	public TokenParser(TokenParserPeer<T, S> peer) {
		this.peer = peer;
	}

	public void flushRunContext(boolean end) throws TokenParserException {
		ParserContext context = peer.getContext();
		context.textsToRanges();
		context.content = TokenParserUtils
				.quickNormalisePunctuation(context.content);
		String content = context.content;
		if (content.contains(debugMarker)) {
			int debug = 3;
		}
		boolean multiLine = peer.isMultiLine();
		if (peer.ignoreCitationsInBlock(content)) {
			peer.resetContext(end || !multiLine);
			return;
		}
		S next = null;
		int length = context.matched.size();
		do {
			if (context.exit) {
				break;
			}
			while ((next = validSequence()) != null) {
				context.addMatchedToken(next);
			}
		} while (peer.processMatch());
		peer.resetContext(!multiLine);
		context = peer.getContext();
		if (context.matched.size() > length) {
			context.lastBlockMatch = context.lastToken();
		} else {
			context.lastBlockMatch = null;
		}
	}
	private S validSequence() throws TokenParserException {
		return validSequence(true);
	}
	private S validSequence(boolean emitOnMatch) throws TokenParserException {
		ParserContext<T, S> context = peer.getContext();
		int minOffset = 99999;
		S bestMatch = null;
		T lastToken = context.lastToken();
		if (lastToken != null && lastToken.isStopToken(context)) {
			return null;
		}
		// tokens can be greedy (all except AT,
		// SEQUENCE_STOP_UNITALICISED_CASE_TITLE)
		// they only influence who gets the best match
		for (T t : peer.getTokens()) {
			int offset = context.startOffset;
			context.provisionalBestMatch = bestMatch;
			S match = (S) t.matchWithFollowCheck(context);
			if (match != null) {
				int startOffsetInRun = match.startOffsetInRun;
				if (trace) {
					System.out.format("Found - %s - %s\n", t, startOffsetInRun);
				}
				boolean overrideCurrent = startOffsetInRun == minOffset
						&& t.overridesAtSameLocation(bestMatch);
				if (startOffsetInRun < minOffset || overrideCurrent) {
					bestMatch = match;
					minOffset = startOffsetInRun;
					context.startOffset = offset;
					if (startOffsetInRun == 0
							&& t.isGreedy(context, bestMatch)) {
						if (!context.matched.isEmpty()
								&& t.shouldStartNewSequence(context)) {
							return null;
						}
						break;
					}
				}
			} else {
			}
			context.startOffset = offset;
		}
		if (trace) {
			System.out.format("validSequence - offset %s\n",
					context.startOffset);
		}
		if (bestMatch != null && (context
				.getCurrentTextRangeEnd() <= context.startOffset + minOffset)) {
			bestMatch = null;
		}
		if (bestMatch != null) {
			if (!context.matched.isEmpty() && minOffset != 0
					&& bestMatch.token.isGreedy(context, bestMatch)) {
				bestMatch = null;
			} else {
				bestMatch = peer.validateMatch(bestMatch);
				// now move cursor
				// past end
			}
		} else {
			// hacky - say we have s52 --- [11] (footnote) -- <i>case title</i>
			// for the moment, just skip back - but this is really a more
			// complicated regex-like lookahead prob
			int offset = context.startOffset;
			if (context.moveToNextRange()) {
				S nextRangeMatch = validSequence(false);
				if (nextRangeMatch != null) {
					bestMatch = nextRangeMatch;
				} else {
					context.startOffset = offset;
				}
			}
		}
		if (bestMatch != null && emitOnMatch) {
			try {
				bestMatch.token.onMatch(context, bestMatch);
			} catch (RuntimeException e) {
				throw new TokenParserException(e);
			}
		}
		return bestMatch;
	}

	public void parse(Document doc) {
		TreeWalker walker = ((DocumentTraversal) doc).createTreeWalker(
				doc.getDocumentElement(),
				NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT, null, true);
		// use walker not iterator - so we can pull/push (for xpath
		// optimisation) w/o exceptions
		Node n = null;
		peer.resetContext(true);
		SurroundingBlockTuple lastSurroundingTuple = null;
		while ((n = walker.nextNode()) != null) {
			ParserContext<T, S> context = peer.getContext();
			SurroundingBlockTuple surroundingTuple = null;
			surroundingTuple = peer.getSurroundingBlockTuple(n);
			if (surroundingTuple == null) {
				surroundingTuple = XmlUtils.getSurroundingBlockTuple(n);
			}
			if (peer.ignoreNode(n)) {
				continue;
			}
			if (surroundingTuple != null) {
				if (!surroundingTuple.equals(lastSurroundingTuple)) {
					if (lastSurroundingTuple != null) {
						short posCompared = lastSurroundingTuple.firstNode
								.compareDocumentPosition(
										surroundingTuple.firstNode);
						if ((posCompared
								& Node.DOCUMENT_POSITION_PRECEDING) > 0) {
							if(peer.ignorePrecedingExceptions()){
								
							}else{
							throw new RuntimeException(
									"Surround tuple before lastSurroundingTuple");
							}
						}
					}
					if (peer.continueBlock(lastSurroundingTuple,
							surroundingTuple)) {
					} else {
						if (!context.allTexts.isEmpty()) {
							peer.flushRunContextAndCatch(false);
							context = peer.getContext();
						}
						surroundingTuple.resetWalker();
						Text txt = surroundingTuple.getCurrentTextChildAndIncrement();
						if (XmlUtils.getContainingBlock(txt) == XmlUtils
								.getContainingBlock(n)) {
							context.reverseItalicBehaviour = peer
									.reverseItalicsForBlocklikeAround(txt,
											surroundingTuple);
						}
					}
				}
				if (lastSurroundingTuple != null) {
					if (lastSurroundingTuple.range != null) {
						lastSurroundingTuple.range.detach();
					}
				}
				lastSurroundingTuple = surroundingTuple;
			}
			switch (n.getNodeType()) {
			case Node.ELEMENT_NODE:
				break;
			case Node.TEXT_NODE:
				boolean handleAsNormal = true;
				// deals with inverted italisication (citations within cited
				// text)
				// need some examples here, to provide tests
				Text t = (Text) n;
				boolean bold = peer.isBold(n);
				boolean emphasised = peer.isEmphasised((Text) n);
				context.addText(t, emphasised, bold);
			}
		}
		if (lastSurroundingTuple != null) {
			if (lastSurroundingTuple.range != null) {
				lastSurroundingTuple.range.detach();
			}
		}
		peer.flushRunContextAndCatch(true);
	}
}
