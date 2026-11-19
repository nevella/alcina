package cc.alcina.framework.gwt.client.dirndl.overlay;

import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 *
 * FIXME - ui2 - 1 - add param to 'only show after 1sec', etc - and update the
 * animation
 */
@Directed(tag = "spinner")
@TypeSerialization(reflectiveSerializable = false, flatSerializable = false)
public class Spinner extends Model.Fields {
	public static Builder builder() {
		return new Spinner().new Builder();
	}

	Builder builder;

	@Directed
	Object inner = new Object();

	@Directed
	String message;

	Spinner() {
	}

	public Spinner generate() {
		return this;
	}

	public class Builder {
		Builder() {
			builder = this;
		}

		public Spinner generate() {
			return Spinner.this.generate();
		}

		public Builder showingMessage(String message) {
			Spinner.this.message = message;
			return this;
		}
	}
}
