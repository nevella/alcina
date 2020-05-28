package cc.alcina.framework.servlet.knowns;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.csobjects.KnownNodeMetadata;
import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;
import cc.alcina.framework.common.client.csobjects.KnownStatusRule;
import cc.alcina.framework.common.client.csobjects.KnownStatusRuleName;
import cc.alcina.framework.common.client.csobjects.KnownTag;
import cc.alcina.framework.common.client.csobjects.KnownTagAlcina;
import cc.alcina.framework.common.client.csobjects.OpStatus;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.TimeConstants;

@RegistryLocation(registryPoint = KnownStatusRuleHandler.class)
public abstract class KnownStatusRuleHandler {
	public abstract KnownStatusRuleName getRuleName();

	public abstract void handleRule(Field field, KnownRenderableNode node,
			KnownStatusRule rule);

	public abstract void handleRule(KnownNodeMetadata nodeMetadata,
			KnownRenderableNode node, KnownStatusRule rule);

	public static class KnownStatusRuleHandler_Max_Age
			extends KnownStatusRuleHandler {
		@Override
		public KnownStatusRuleName getRuleName() {
			return KnownStatusRuleName.Max_Age;
		}

		@Override
		public void handleRule(Field field, KnownRenderableNode node,
				KnownStatusRule rule) {
			Preconditions.checkArgument(field.getType() == KnownJob.class);
			KnownJob value = (KnownJob) node.typedValue;
			if (value == null) {
				return;
			}
			Date lastOkDate = value.lastOk;
			KnownTagAlcina status = null;
			if (value.status != null) {
				switch (value.status) {
				case FAILED:
					status = KnownTagAlcina.Status_Error;
					break;
				}
			}
			if (status == KnownTagAlcina.Status_Error) {
				// error is error. ignore last ok
			} else {
				if (lastOkDate != null) {
					status = KnownTagAlcina.Status_Ok;
					if (System.currentTimeMillis() - lastOkDate.getTime() > rule
							.warnValue() * TimeConstants.ONE_HOUR_MS) {
						status = KnownTagAlcina.Status_Warn;
					}
					if (System.currentTimeMillis() - lastOkDate.getTime() > rule
							.errorValue() * TimeConstants.ONE_HOUR_MS) {
						status = KnownTagAlcina.Status_Error;
					}
				}
			}
			if (status != null) {
				node.tags.add(status);
			}
		}

		@Override
		public void handleRule(KnownNodeMetadata nodeMetadata,
				KnownRenderableNode node, KnownStatusRule rule) {
			KnownTagAlcina status = null;
			OpStatus opStatus = null;
			if (node.hasProperty("status")) {
				opStatus = node.typedChildValue(OpStatus.class, "status");
			}
			if (opStatus != null) {
				switch (opStatus) {
				case FAILED:
					status = KnownTagAlcina.Status_Error;
					break;
				}
			}
			if (status == KnownTagAlcina.Status_Error) {
				// error is error. ignore last ok
			} else {
				if (node.hasProperty("lastOk") || node.hasProperty("lastRun")) {
					Date lastOkDate = node.hasProperty("lastOk")
							? node.typedChildValue(Date.class, "lastOk")
							: node.typedChildValue(Date.class, "lastRun");
					if (lastOkDate != null) {
						status = KnownTagAlcina.Status_Ok;
						if (System.currentTimeMillis()
								- lastOkDate.getTime() > rule.warnValue()
										* TimeConstants.ONE_HOUR_MS) {
							status = KnownTagAlcina.Status_Warn;
						}
						if (System.currentTimeMillis()
								- lastOkDate.getTime() > rule.errorValue()
										* TimeConstants.ONE_HOUR_MS) {
							status = KnownTagAlcina.Status_Error;
						}
					}
				}
			}
			if (status != null) {
				node.tags.add(status);
			}
		}
	}

	public static class KnownStatusRuleHandler_Run_By
			extends KnownStatusRuleHandler {
		@Override
		public KnownStatusRuleName getRuleName() {
			return KnownStatusRuleName.Run_By;
		}

		@Override
		public void handleRule(Field field, KnownRenderableNode node,
				KnownStatusRule rule) {
			Preconditions.checkArgument(field.getType() == KnownJob.class);
			KnownJob value = (KnownJob) node.typedValue;
			if (value == null) {
				return;
			}
			Date lastOkDate = value.lastOk;
			KnownTagAlcina status = null;
			if (value.status != null) {
				switch (value.status) {
				case FAILED:
					status = KnownTagAlcina.Status_Error;
					break;
				}
			}
			if (status == KnownTagAlcina.Status_Error) {
				// error is error. ignore last ok
			} else {
				if (lastOkDate != null) {
					status = checkDateTime(rule, lastOkDate);
				}
			}
			if (status != null) {
				node.tags.add(status);
			}
		}

		@Override
		public void handleRule(KnownNodeMetadata nodeMetadata,
				KnownRenderableNode node, KnownStatusRule rule) {
			KnownTagAlcina status = null;
			OpStatus opStatus = null;
			if (node.hasProperty("status")) {
				opStatus = node.typedChildValue(OpStatus.class, "status");
			}
			if (opStatus != null) {
				switch (opStatus) {
				case FAILED:
					status = KnownTagAlcina.Status_Error;
					break;
				}
			}
			if (status == KnownTagAlcina.Status_Error) {
				// error is error. ignore last ok
			} else {
				if (node.hasProperty("lastOk") || node.hasProperty("lastRun")) {
					Date lastOkDate = node.hasProperty("lastOk")
							? node.typedChildValue(Date.class, "lastOk")
							: node.typedChildValue(Date.class, "lastRun");
					if (lastOkDate != null) {
						status = checkDateTime(rule, lastOkDate);
					}
				}
			}
			if (status != null) {
				node.tags.add(status);
			}
		}

		private KnownTagAlcina checkDateTime(
				KnownStatusRule rule, Date lastOkDate) {
			KnownTagAlcina status = KnownTagAlcina.Status_Ok;
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime lastOkLt = LocalDateTime.ofInstant(
					lastOkDate.toInstant(), ZoneId.systemDefault());
			// Warn if not run by HOUR_OF_DAY(rule.warnValue())
			if (now.getDayOfYear() == lastOkLt.getDayOfYear() &&
					now.getHour() >= rule.warnValue()) {
				status = KnownTagAlcina.Status_Warn;
			}
			// Err if not run by HOUR_OF_DAY(rule.errorValue())
			if (now.getDayOfYear() == lastOkLt.getDayOfYear() &&
					now.getHour() >= rule.errorValue()) {
				status = KnownTagAlcina.Status_Error;
			}
			return status;
		}
	}
}
