package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestion;

/**
 * Filters a list of models by a string query, and returns a corresponding list
 * of Suggestion.Default elements (marked up by which parts of the model string
 * representation match the query)
 *
 * @author nick@alcina.cc
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
				Suggestion.Default suggestion = new Suggestion.Default();
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
}