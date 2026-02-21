package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import java.util.List;

import cc.alcina.framework.gwt.client.dirndl.event.Filterable;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Answers;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.Suggestion.ModelSuggestion;

/**
 * Filters a list of Filterable by a string query
 *
 * @param <T>
 */
public class FilterableAskAnswer<T extends Filterable> {
	public Suggestor.Answers ask(Suggestor.StringAsk ask, List<T> models) {
		Suggestor.Answers result = new Answers(ask);
		result.ask = ask;
		models.stream().filter(t -> t.matchesFilter(ask.getValue()))
				.map(ModelSuggestion::new).forEach(suggestion -> result
						.add(suggestion, ask.getResultRange()));
		return result;
	}
}