package cc.alcina.framework.gwt.client.dirndl.overlay;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 *
 * FIXME - ui2 - 1 - add param to 'only show after 1sec', etc - and update the
 * animation
 */
@Directed.Multiple({ @Directed(tag = "spinner"), @Directed(tag = "inner") })
public class Spinner extends Model {
	public static Builder builder() {
		return new Builder();
	}

	@SuppressWarnings("unused")
	private Builder builder;

	Spinner(Builder builder) {
		this.builder = builder;
	}

	public Spinner generate() {
		return this;
	}

	public static class Builder {
		public Spinner generate() {
			return new Spinner(this).generate();
		}
	}
}
