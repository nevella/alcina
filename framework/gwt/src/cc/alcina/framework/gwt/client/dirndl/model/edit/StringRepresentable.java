package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.ContextSensitiveReverseTransform;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.ContextSensitiveTransform;

public class StringRepresentable {
	/**
	 * Transforms the referenced object to/from a string representation (which
	 * will populate the decorated node's <code>uid</code> field)
	 */
	public static class RepresentableToStringTransform<SR>
			implements Binding.Bidi<SR> {
		@Override
		public Function<SR, String> leftToRight() {
			return new Left();
		}

		@Override
		public Function<String, SR> rightToLeft() {
			return new Right();
		}

		public interface ToStringRepresentation<SR>
				extends ContextSensitiveTransform<SR> {
		}

		public interface FromStringRepresentation<SR>
				extends ContextSensitiveReverseTransform<SR> {
		}

		public interface HasStringRepresentableType<SR> {
			Class<SR> stringRepresentableType();
		}

		class Left extends Binding.AbstractContextSensitiveTransform<SR> {
			@Override
			public String apply(SR t) {
				if (t == null) {
					return null;
				} else {
					ToStringRepresentation<SR> impl = Registry
							.impl(ToStringRepresentation.class, t.getClass());
					impl.withContextNode(node);
					return impl.apply(t);
				}
			}
		}

		class Right
				extends Binding.AbstractContextSensitiveReverseTransform<SR> {
			@Override
			public SR apply(String t) {
				if (t == null) {
					return null;
				} else {
					HasStringRepresentableType contextModel = node.getModel();
					FromStringRepresentation<SR> impl = Registry.impl(
							FromStringRepresentation.class,
							contextModel.stringRepresentableType());
					impl.withContextNode(node);
					return (SR) impl.apply(t);
				}
			}
		}

		@Registration({ ToStringRepresentation.class, String.class })
		public static class PassthroughTransformLeft
				extends Binding.AbstractContextSensitiveTransform<String>
				implements ToStringRepresentation<String> {
			@Override
			public String apply(String t) {
				return t;
			}
		}

		@Registration({ FromStringRepresentation.class, String.class })
		public static class PassthroughTransformRight
				extends Binding.AbstractContextSensitiveReverseTransform<String>
				implements FromStringRepresentation<String> {
			@Override
			public String apply(String t) {
				return t;
			}
		}
	}
}
