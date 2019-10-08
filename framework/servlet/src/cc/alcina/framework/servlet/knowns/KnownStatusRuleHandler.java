package cc.alcina.framework.servlet.knowns;

import java.lang.reflect.Field;
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

	public abstract void handleRule(KnownNodeMetadata nodeMetadata,
			KnownRenderableNode node, KnownStatusRule rule);
}
