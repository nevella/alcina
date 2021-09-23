package cc.alcina.framework.gwt.client.gwittir.widget;

import java.io.Serializable;

import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

public interface BoundSuggestOracleResponseType {
	default String toSuggestionResultString() {
		return toSuggestionString();
	}

	String toSuggestionString();

	public interface BoundSuggestOracleModel extends Serializable {
	}

	public static class BoundSuggestOracleModelNoop
			implements BoundSuggestOracleModel {
	}

	public static class BoundSuggestOracleSuggestion
			implements Suggestion, Serializable {
		public static Object nullSuggestion() {
			BoundSuggestOracleSuggestion suggestion = new BoundSuggestOracleSuggestion();
			suggestion.displayString = "(empty)";
			return suggestion;
		}

		private String displayString;

		public BoundSuggestOracleResponseType typedValue;

		public BoundSuggestOracleSuggestion() {
		}

		public BoundSuggestOracleSuggestion(
				BoundSuggestOracleResponseType typedValue) {
			this.typedValue = typedValue;
			displayString = typedValue.toSuggestionString();
		}

		@Override
		public String getDisplayString() {
			return displayString;
		}

		@Override
		public String getReplacementString() {
			return null;
		}
	}
}