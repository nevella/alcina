package cc.alcina.framework.gwt.client.dirndl.overlay;

import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed
public class Caught extends Model {
	public static Builder builder(Throwable caught) {
		return new Builder(caught);
	}

	private Builder builder;

	Caught(Builder builder) {
		this.builder = builder;
	}

	@Directed
	public String getMessage() {
		return "There's been a problem with the requested operation.\n"
				+ (Permissions.get().isAdmin() ? builder.toString() : "");
	}

	public static class Builder {
		boolean mustBeLoggedIn;

		String message;

		Throwable caught;

		public Builder(Throwable caught) {
			this.caught = caught;
		}

		public Caught build() {
			return new Caught(this);
		}

		@Override
		public String toString() {
			FormatBuilder fb = new FormatBuilder().separator("\n");
			fb.appendIfNotBlank(CommonUtils.toSimpleExceptionMessage(caught),
					message);
			return fb.toString();
		}

		public Builder withMessage(String message) {
			this.message = message;
			return this;
		}

		public Builder withMustBeLoggedIn(boolean mustBeLoggedIn) {
			this.mustBeLoggedIn = mustBeLoggedIn;
			return this;
		}
	}
}
