package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

/**
 * A panel heading with actions
 */
public class HeadingActions extends Model.All {
	@Directed
	public String heading;

	@Directed.Wrap("actions")
	public List<? super Model> actions = new ArrayList<>();

	public HeadingActions() {
	}

	public HeadingActions(String heading) {
		this.heading = heading;
	}
}
