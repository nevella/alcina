package cc.alcina.framework.gwt.client.gwittir.widget;

import java.io.Serializable;

import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

public interface BoundSuggestOracleResponseType {
	String toSuggestionString();
	
	public static class BoundSuggestOracleSuggestion implements Suggestion,Serializable {
		@Override
		public String getDisplayString() {
			return typedValue.toSuggestionString();
		}
		public BoundSuggestOracleSuggestion() {
		}
		public BoundSuggestOracleSuggestion(BoundSuggestOracleResponseType typedValue) {
			this.typedValue = typedValue;
		}
		

		@Override
		public String getReplacementString() {
			return null;
		}

		public BoundSuggestOracleResponseType typedValue;
	}
}