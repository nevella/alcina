package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

@Reflected
// Obviously similar to JobStatus - but broader applicability
public enum Status implements ProvidesStatus {
	OK, WARN, ERROR;

	public static Status
			provideMultiple(List<? extends ProvidesStatus> children) {
		return children.stream().map(ProvidesStatus::provideStatus)
				.max(Comparator.naturalOrder()).orElse(OK);
	}

	@Override
	public Status provideStatus() {
		return this;
	}

	@Directed
	public static class StatusReason extends Model implements ProvidesStatus {
		public static StatusReason check(boolean condition,
				String successReason, String failureReason) {
			if (condition) {
				return ok(successReason);
			} else {
				return error(failureReason, failureReason);
			}
		}

		public StatusReason() {
		}

		public static StatusReason error(String reason) {
			return error(null, reason);
		}

		public static StatusReason error(Object value, String reason) {
			return new StatusReason(Status.ERROR, reason, value);
		}

		public static StatusReason forDate(Date date, String issueReason,
				long maxAge) {
			if (maxAge == Long.MAX_VALUE || (date != null
					&& TimeConstants.within(date.getTime(), maxAge))) {
				return ok(date);
			} else {
				return error(date, issueReason);
			}
		}

		public static StatusReason forDateMissingWithStatus(Date date,
				Status status) {
			return date == null ? missingWithStatus(status) : ok(date);
		}

		public static StatusReason fromChildStatus(Status status) {
			return new StatusReason(status, "From children", status);
		}

		public static StatusReason missing() {
			return missingWithStatus(ERROR);
		}

		public static StatusReason missingWithStatus(Status status) {
			return new StatusReason(status, "Empty/null data",
					"Empty/null data");
		}

		public static StatusReason ok(Object value) {
			if (value == null) {
				value = "OK";
			}
			String reason = value.toString();
			return new StatusReason(Status.OK, reason, value);
		}

		public static StatusReason warn(Object value, String reason) {
			return new StatusReason(Status.WARN, reason, value);
		}

		private Status status;

		public void setStatus(Status status) {
			this.status = status;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		private String reason;

		private transient Object value;

		public StatusReason(Status status, String reason, Object value) {
			this.status = status;
			this.reason = reason;
			this.value = value;
		}

		@Binding(from = "displayText", type = Type.INNER_TEXT)
		public Object getDisplayText() {
			String text = "[null]";
			if (value != null) {
				text = value.toString();
			} else if (reason != null) {
				text = reason;
			}
			return CommonUtils.trimToWsChars(text, 60, true);
		}

		@Binding(to = "title", type = Type.PROPERTY)
		public String getReason() {
			return this.reason;
		}

		@Binding(type = Type.CLASS_PROPERTY)
		public Status getStatus() {
			return this.status;
		}

		@AlcinaTransient
		public Object getValue() {
			return this.value;
		}

		@Override
		public Status provideStatus() {
			return status;
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s", status, getDisplayText());
		}

		@Property.Not
		public boolean isOk() {
			return status == OK;
		}
	}

	@Registration(StatusTransform.class)
	public interface StatusTransform extends ToStringFunction<Status> {
		public static class DefaultImpl implements StatusTransform {
			@Override
			public String apply(Status t) {
				switch (t) {
				case OK:
					return "transparent";
				case WARN:
					return "yellow";
				case ERROR:
					return "red";
				default:
					throw new UnsupportedOperationException();
				}
			}
		}
	}
}