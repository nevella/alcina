package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.Notification.Builder.ThrowableBuilder;
import cc.alcina.framework.gwt.client.dirndl.overlay.Notification.Builder.WarnBuilder;
import cc.alcina.framework.gwt.client.logic.MessageManager;

@Directed(bindings = @Binding(from = "className", type = Type.PROPERTY))
public class Notification extends Model {
	public static Builder builder(Model model) {
		return new Builder(model);
	}

	public static Builder builder(String message) {
		return new Builder(message);
	}

	public static ThrowableBuilder caught(Throwable throwable) {
		return new ThrowableBuilder(throwable);
	}

	public static Notification show(String string) {
		return builder(string).show();
	}

	public static WarnBuilder warn(String message) {
		return new WarnBuilder(message);
	}

	String className;

	String message;

	VariantStyle variantStyle;

	Model model;

	Notification() {
	}

	public String getClassName() {
		return this.className;
	}

	@Directed
	public String getMessage() {
		return this.message;
	}

	@Directed
	public Model getModel() {
		return this.model;
	}

	public void show() {
		// Future-proofing for more complex notifications by abstracting (and
		// having Notification be a Model) - but for the mo' just route via
		// MessageManager
		switch (variantStyle) {
		case caught:
		case warn:
			MessageManager.topicExceptionMessagePublished.publish(message);
			break;
		case notification:
			MessageManager.topicMessagePublished.publish(message);
			break;
		case model:
			// will have different styling
		case dialog:
			MessageManager.topicNotificationModelPublished.publish(this);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static class Builder<B extends Builder> {
		boolean mustBeLoggedIn;

		String message;

		boolean modal;

		List<String> classNames = new ArrayList<>();

		VariantStyle variantStyle;

		Model model;

		protected Builder() {
		}

		public Builder(Model model) {
			this.model = model;
			variantStyle = VariantStyle.model;
		}

		public Builder(String message) {
			this.message = message;
		}

		public B addClassName(Object className) {
			classNames.add(className.toString());
			return (B) this;
		}

		public Notification build() {
			Notification notification = new Notification();
			notification.message = message;
			notification.model = model;
			notification.className = getClassName();
			notification.variantStyle = variantStyle;
			return notification;
		}

		protected String getClassName() {
			return classNames.isEmpty() ? null
					: classNames.stream().collect(Collectors.joining(" "));
		}

		public Notification show() {
			Notification notification = build();
			notification.show();
			return notification;
		}

		public B withMessage(String message) {
			this.message = message;
			return (B) this;
		}

		public B withModal(boolean modal) {
			this.modal = modal;
			return (B) this;
		}

		public B withModel(Model model) {
			this.model = model;
			return (B) this;
		}

		public static class ThrowableBuilder extends Builder<ThrowableBuilder> {
			Throwable throwable;

			public ThrowableBuilder(Throwable throwable) {
				this.throwable = throwable;
				modal = true;
				addClassName(VariantStyle.caught);
				variantStyle = VariantStyle.caught;
			}

			@Override
			public String toString() {
				FormatBuilder fb = new FormatBuilder().separator("\n");
				fb.appendIfNotBlank(
						CommonUtils.toSimpleExceptionMessage(throwable),
						message);
				return fb.toString();
			}

			public ThrowableBuilder withMustBeLoggedIn(boolean mustBeLoggedIn) {
				this.mustBeLoggedIn = mustBeLoggedIn;
				return this;
			}
		}

		public static class WarnBuilder extends Builder<WarnBuilder> {
			public WarnBuilder(String message) {
				super(message);
				addClassName(VariantStyle.warn);
				variantStyle = VariantStyle.warn;
			}
		}
	}

	/**
	 * A dismissable message container (of arbitrary complexity) for use inside
	 * a Notification
	 *
	 * 
	 *
	 */
	@Directed
	public static class DismisableMessage extends Model {
		private final Model model;

		private final String x = "x";

		public DismisableMessage(Model model) {
			this.model = model;
		}

		@Directed
		public Model getModel() {
			return this.model;
		}

		@Directed(reemits = { DomEvents.Click.class, ModelEvents.Close.class })
		public String getX() {
			return this.x;
		}
	}

	public enum VariantStyle {
		notification, caught, warn, model, dialog
	}
}
