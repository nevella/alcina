package cc.alcina.framework.entity.util;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringEscapeUtils;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;

public class CodeBuilder extends FormatBuilder {
	public static String asStringLiteral(String value) {
		return Ax.format("\"%s\"", StringEscapeUtils.escapeJava(value));
	}

	protected boolean completed;

	private boolean statement;

	protected void complete() {
		if (isStatement()) {
			append(";");
		}
	}

	public boolean isStatement() {
		return this.statement;
	}

	private void maybeComplete() {
		if (!completed) {
			completed = true;
			complete();
		}
	}

	public void setStatement(boolean statement) {
		this.statement = statement;
	}

	@Override
	public String toString() {
		maybeComplete();
		return super.toString();
	}

	public static class AnnotationDeclaration extends CodeBuilder {
		private int attributeCount = 0;

		private Class<? extends Annotation> annotationType;

		public AnnotationDeclaration(
				Class<? extends Annotation> annotationClass) {
			this.annotationType = annotationClass;
			append("@");
			append(annotationClass.getSimpleName());
			append("(");
		}

		/**
		 * Add an annotation attribute
		 */
		public void add(String name, Enum e) {
			addNameValuePair(name, e,
					e.getClass().getSimpleName() + "." + e.toString());
		}

		public void add(String name, List<String> strings) {
			String[] stringArray = (String[]) strings
					.toArray(new String[strings.size()]);
			ArrayBuilder representationBuilder = new ArrayBuilder();
			strings.forEach(representationBuilder::add);
			addNameValuePair(name, stringArray,
					representationBuilder.toString());
		}

		public void add(String name, String value) {
			value = Ax.blankToEmpty(value);
			addNameValuePair(name, value, asStringLiteral(value));
		}

		private void addNameValuePair(String name, Object value,
				String valueRepresentation) {
			if (isDefaultValue(name, value)) {
				return;
			}
			if (attributeCount++ > 0) {
				separator(",");
			}
			append(name);
			separator("=");
			append(valueRepresentation);
			separator("");
		}

		@Override
		protected void complete() {
			append(")");
			super.complete();
		}

		private boolean isDefaultValue(String name, Object o) {
			try {
				Object defaultValue = annotationType
						.getDeclaredMethod(name, new Class[0])
						.getDefaultValue();
				return Objects.deepEquals(o, defaultValue);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public static class ArrayBuilder extends CodeBuilder {
		public ArrayBuilder() {
			separator("");
			append("{");
		}

		void add(Object object) {
			if (object instanceof String) {
				append(asStringLiteral((String) object));
			} else {
				throw new UnsupportedOperationException();
			}
			separator(",");
		}

		@Override
		protected void complete() {
			separator("");
			append("}");
			super.complete();
		}

		@Override
		public ArrayBuilder separator(String separator) {
			return (ArrayBuilder) super.separator(separator);
		}
	}
}
