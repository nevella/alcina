package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient.TransienceContext;
import cc.alcina.framework.common.client.util.LooseContext;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.METHOD })
@ClientVisible
public @interface AlcinaTransient {
	// empty implies 'transient in all contexts' - non empty restricts
	// transience to the selected contexts
	TransienceContext[] value() default {};

	public enum TransienceContext {
		ALL, CLIENT, RPC, JOB, SERVER
	}

	public static class Support {
		private static final String CONTEXT_TRANSIENCE_CONTEXTS = AlcinaTransient.Support.class
				.getName() + ".CONTEXT_TRANSIENCE_CONTEXTS";

		public static void setContextTypes(TransienceContext... contexts) {
			LooseContext.set(CONTEXT_TRANSIENCE_CONTEXTS, contexts);
		}
		public static TransienceContext[] getContextTypes() {
			TransienceContext[] types = LooseContext.get(CONTEXT_TRANSIENCE_CONTEXTS);
			if (types == null) {
				types = new TransienceContext[0];
			}
			return types;
		}

		public static boolean isTransient(AlcinaTransient annotation,
				TransienceContext... types) {
			if (annotation == null) {
				return false;
			}
			List<TransienceContext> list = Arrays.asList(annotation.value());
			if (list.isEmpty()) {
				return true;
			}
			return Arrays.stream(types).anyMatch(list::contains);
		}

		public static boolean isContextTransient(AlcinaTransient annotation) {
			return isTransient(annotation,getContextTypes());
		}

		public static void checkNoContextTrasience() {
			Preconditions.checkState(!LooseContext.has(CONTEXT_TRANSIENCE_CONTEXTS));
		}
	}
}
