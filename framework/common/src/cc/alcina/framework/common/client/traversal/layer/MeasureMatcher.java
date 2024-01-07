package cc.alcina.framework.common.client.traversal.layer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;

/**
 * Matches the inputstate inputcontent against a regex. Currently jdk only
 */
public class MeasureMatcher {
	private ParserState parserState;

	public MeasureMatcher(ParserState parserState) {
		this.parserState = parserState;
	}

	public Measure match(Token token, Pattern pattern) {
		CharSequence text = parserState.inputContent();
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			return parserState.input.subMeasure(
					parserState.getOffsetInInput() + matcher.start(),
					parserState.getOffsetInInput() + matcher.end(), token,
					true);
		} else {
			return null;
		}
	}
}