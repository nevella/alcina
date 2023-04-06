package cc.alcina.framework.common.client.traversal.layer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.traversal.layer.LayerParser.InputState;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;

public class MeasureMatcher {
	private InputState inputState;

	public MeasureMatcher(InputState inputState) {
		this.inputState = inputState;
	}

	public Measure match(Token token, Pattern pattern) {
		String text = inputState.inputContent();
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			return inputState.input.subMeasure(
					inputState.getOffsetInInput() + matcher.start(),
					inputState.getOffsetInInput() + matcher.end(), token);
		} else {
			return null;
		}
	}
}