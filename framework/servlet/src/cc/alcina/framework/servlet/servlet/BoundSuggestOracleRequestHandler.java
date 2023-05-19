package cc.alcina.framework.servlet.servlet;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.user.client.ui.SuggestOracle.Response;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.entity.projection.GraphProjections;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox.BoundSuggestOracleRequest;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleModel;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseType.BoundSuggestOracleSuggestion;

@Registration.NonGenericSubtypes(BoundSuggestOracleRequestHandler.class)
public abstract class BoundSuggestOracleRequestHandler<T extends BoundSuggestOracleResponseType> {
	public Response handleRequest(Class<T> clazz,
			BoundSuggestOracleRequest request, String hint) {
		Response response = new Response();
		List<T> responses = getResponses(request.getQuery(), request.getModel(),
				hint);
		responses = projectResponses(responses, clazz);
		response.setSuggestions(responses.stream()
				.map(BoundSuggestOracleSuggestion::new)
				.limit(getSuggestionLimit()).collect(Collectors.toList()));
		if (offerNullSuggestion()) {
			((List) response.getSuggestions()).add(0,
					BoundSuggestOracleSuggestion.nullSuggestion());
		}
		return projectResponse(response);
	}

	protected abstract List<T> getResponses(String query,
			BoundSuggestOracleModel model, String hint);

	protected long getSuggestionLimit() {
		return 50;
	}

	protected boolean offerNullSuggestion() {
		return true;
	}

	protected Response projectResponse(Response response) {
		return GraphProjections.defaultProjections().project(response);
	}

	protected List<T> projectResponses(List<T> list, Class<T> clazz) {
		if (Entity.class.isAssignableFrom(clazz)) {
			list = GraphProjections.defaultProjections().maxDepth(1)
					.project(list);
		}
		return list;
	}
}