package cc.alcina.framework.servlet.knowns;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.csobjects.KnownNodeMetadata;
import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;
import cc.alcina.framework.common.client.csobjects.KnownStatusRule;
import cc.alcina.framework.common.client.csobjects.KnownStatusRuleName;
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
			KnownTagAlcina status = testRule(value.status, rule, value.lastOk);
			if (status != null) {
				node.tags.add(status);
			}
		}

		@Override
		public void handleRule(KnownNodeMetadata nodeMetadata,
				KnownRenderableNode node, KnownStatusRule rule) {
			OpStatus opStatus = null;
			Date lastOkDate = null;
			if (node.hasProperty("status")) {
				opStatus = node.typedChildValue(OpStatus.class, "status");
			}
			if (node.hasProperty("lastOk") || node.hasProperty("lastRun")) {
				lastOkDate = node.hasProperty("lastOk")
						? node.typedChildValue(Date.class, "lastOk")
						: node.typedChildValue(Date.class, "lastRun");
			}
			KnownTagAlcina status = testRule(opStatus, rule, lastOkDate);
			if (status != null) {
				node.tags.add(status);
			}
		}

		private KnownTagAlcina testRule(OpStatus opStatus, KnownStatusRule rule,
				Date lastOkDate) {
			if (opStatus == OpStatus.FAILED) {
				return KnownTagAlcina.Status_Error;
			} else if (lastOkDate != null) {
				return checkDateTime(rule, lastOkDate);
			}
			return null;
		}

		private KnownTagAlcina checkDateTime(KnownStatusRule rule,
				Date lastOkDate) {
			KnownTagAlcina status = KnownTagAlcina.Status_Ok;
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime lastOkLt = LocalDateTime
					.ofInstant(lastOkDate.toInstant(), ZoneId.systemDefault());
			long daysPassed = ChronoUnit.DAYS.between(lastOkLt, now);
			// Warn if not run by HOUR_OF_DAY(rule.warnValue()) the day after
			// last run
			if (daysPassed == 1 && now.getHour() >= rule.warnValue()) {
				status = KnownTagAlcina.Status_Warn;
			}
			// Err if not run by HOUR_OF_DAY(rule.errorValue()) the day after
			// last run
			if (daysPassed == 1 && now.getHour() >= rule.errorValue()) {
				status = KnownTagAlcina.Status_Error;
			}
			// Err if not run on the same day and the expected run time has
			// passed
			if (daysPassed > 1) {
				status = KnownTagAlcina.Status_Error;
			}
			return status;
		}
	}
}
