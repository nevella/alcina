package cc.alcina.framework.gwt.client.gwittir.widget;

import java.io.Serializable;

import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;

@ReflectiveSerializer.Checks(hasReflectedSubtypes = true)
public interface BoundSuggestOracleResponseElement {
	default String toSuggestionResultString() {
		return toSuggestionString();
	}

	String toSuggestionString();

	@ReflectiveSerializer.Checks(hasReflectedSubtypes = true)
	public interface BoundSuggestOracleModel extends Serializable {
	}

	public static class BoundSuggestOracleModelNoop
			implements BoundSuggestOracleModel {
	}

	public static class UntypedSuggestion extends Bindable.Fields
			implements Suggestion, Serializable {
		public UntypedSuggestion() {
		}

		public UntypedSuggestion(Object suggestion) {
			this.suggestion = suggestion;
		}

		@PropertySerialization(notTestable = true)
		public Object suggestion;

		@Override
		public String getDisplayString() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'getDisplayString'");
		}

		@Override
		public String getReplacementString() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'getReplacementString'");
		}
	}

	public static class BoundSuggestOracleSuggestion extends Bindable
			implements Suggestion, Serializable {
		public static Object nullSuggestion() {
			BoundSuggestOracleSuggestion suggestion = new BoundSuggestOracleSuggestion();
			suggestion.displayString = "(empty)";
			return suggestion;
		}

		private String displayString;

		private BoundSuggestOracleResponseElement typedValue;

		public BoundSuggestOracleSuggestion() {
		}

		public BoundSuggestOracleSuggestion(
				BoundSuggestOracleResponseElement typedValue) {
			this.setTypedValue(typedValue);
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

		public BoundSuggestOracleResponseElement getTypedValue() {
			return typedValue;
		}

		public void setDisplayString(String displayString) {
			this.displayString = displayString;
		}

		public void
				setTypedValue(BoundSuggestOracleResponseElement typedValue) {
			this.typedValue = typedValue;
		}

		@Override
		public String toString() {
			FormatBuilder format = new FormatBuilder().separator(" ");
			format.append(displayString);
			if (typedValue != null) {
				format.format("[%s]", NestedName.get(typedValue));
			}
			return format.toString();
		}
	}
}