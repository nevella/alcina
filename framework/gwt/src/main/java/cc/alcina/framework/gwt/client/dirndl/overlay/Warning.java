package cc.alcina.framework.gwt.client.dirndl.overlay;

public class Warning {
	public static Builder builder() {
		return new Builder();
	}

	private Builder builder;

	Warning(Builder builder) {
		this.builder = builder;
	}

	public Warning show() {
		Notification.show(builder.message);
		return this;
	}

	public static class Builder {
		boolean mustBeLoggedIn;

		String message;

		public Warning show() {
			return new Warning(this).show();
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
