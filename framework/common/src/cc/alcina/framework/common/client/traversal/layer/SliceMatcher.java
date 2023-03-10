package cc.alcina.framework.common.client.traversal.layer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.traversal.layer.LayerParser.InputState;

public class SliceMatcher {
	private InputState inputState;

	public SliceMatcher(InputState inputState) {
		this.inputState = inputState;
	}

	public Slice match(LayerToken token, Pattern pattern) {
		String text = inputState.inputContent();
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			return inputState.input.subSlice(
					inputState.getOffsetInInput() + matcher.start(),
					inputState.getOffsetInInput() + matcher.end(), token);
		} else {
			return null;
		}
	}
}