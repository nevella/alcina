package cc.alcina.framework.entity.entityaccess.metric;

import java.text.SimpleDateFormat;
import java.util.Date;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.util.Ax;

public abstract class StatCategory {
	private Class<? extends StatCategory> parent;

	private String name;

	public StatCategory(Class<? extends StatCategory> parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	public void emit() {
		emit(System.currentTimeMillis());
	}

	public void emit(long time) {
		String timeStamp = new SimpleDateFormat("HH:mm:ss,SSS")
				.format(new Date(time));
		String key = Ax.format("[alc-%s]", getClass().getSimpleName());
		StartupStats.topicEmitStat
				.publish(Ax.format("%s %s :: end", timeStamp, key));
	}

	public boolean isParallel() {
		return false;
	}

	public String name() {
		return name;
	}

	public Class<? extends StatCategory> parent() {
		return parent;
	}

	protected int depth() {
		if (parent == null) {
			return 0;
		}
		return Reflections.newInstance(parent).depth() + 1;
	}
}