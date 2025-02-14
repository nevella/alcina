package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.user.client.ui.SuggestOracle;

import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.StringAsk;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestion;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement.UntypedSuggestion;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

/**
 * Filters a list of models by a string query, and returns a corresponding list
 * of Suggestion.Default elements (marked up by which parts of the model string
 * representation match the query)
 *
 * 
 *
 * @param <T>
 */
public class StringAskAnswer<T> {
	public Suggestor.Answers ask(Suggestor.StringAsk ask, List<T> models,
			Function<T, String> stringRepresentation) {
		Suggestor.Answers result = new Answers();
		models.stream().map(model -> {
			String string = stringRepresentation.apply(model);
			MarkupMatch match = new MarkupMatch(string, ask.getValue());
			if (match.hasMatches()) {
				Suggestion.Markup suggestion = new Suggestion.Markup();
				suggestion.setMarkup(match.toMarkup());
				suggestion.setMatch(true);
				suggestion.setModel(model);
				return suggestion;
			} else {
				return null;
			}
		}).filter(Objects::nonNull).forEach(
				suggestion -> result.add(suggestion, ask.getResultRange()));
		return result;
	}

	/*
	 * Local emulation of an rpc suggest callback (for local values)
	 */
	public static SuggestOracle.Response selectValues(List<?> values,
			StringAsk ask) {
		List<UntypedSuggestion> suggestions = values.stream()
				.filter(v -> SearchUtils.matches(ask.getValue(), v))
				.map(BoundSuggestOracleResponseElement.UntypedSuggestion::new)
				.collect(Collectors.toList());
		return new SuggestOracle.Response(suggestions);
	}
}