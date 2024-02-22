package cc.alcina.framework.gwt.client.dirndl.cmp;

import java.io.Serializable;
import java.util.List;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;

public class AppSuggestorResponse extends SuggestOracle.Response
		implements Serializable {
	public List<? extends AppSuggestionEntry> appSuggestions() {
		return (List<? extends AppSuggestionEntry>) getSuggestions();
	}
}
