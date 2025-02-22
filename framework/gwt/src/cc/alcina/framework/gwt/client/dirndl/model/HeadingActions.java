package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;

/**
 * A panel heading with actions
 */
@TypedProperties
public class HeadingActions extends Model.All {
	public static PackageProperties._HeadingActions properties = PackageProperties.headingActions;

	@Directed
	public String heading;

	@Directed.Wrap("actions")
	public List<? super Model> actions = new ArrayList<>();

	public HeadingActions() {
	}

	public HeadingActions(String heading) {
		this.heading = heading;
	}

	public HeadingActions
			withModelActions(Class<? extends ModelEvent>... eventClasses) {
		Arrays.stream(eventClasses).map(Link::of).forEach(actions::add);
		return this;
	}
}
