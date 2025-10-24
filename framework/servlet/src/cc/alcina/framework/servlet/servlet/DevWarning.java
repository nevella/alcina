package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.FormatBuilder;

@Bean(PropertySource.FIELDS)
public class DevWarning implements ProcessObservable, TreeSerializable {
	public String category;

	public String detail;

	DevWarning() {
	}

	public DevWarning(String category, String detail) {
		this.category = category;
		this.detail = detail;
	}

	@Override
	public String toString() {
		return FormatBuilder.keyValues("category", category, "detail", detail);
	}
}
