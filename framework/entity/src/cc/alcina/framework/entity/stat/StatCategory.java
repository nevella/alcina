package cc.alcina.framework.entity.stat;

import java.text.SimpleDateFormat;
import java.util.Date;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;

public abstract class StatCategory {
	public static final transient String CONTEXT_MUTED = StatCategory.class
			.getName() + ".CONTEXT_MUTED";

	private Class<? extends StatCategory> parent;

	private String name;

	public StatCategory(Class<? extends StatCategory> parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	public void emit() {
		emit(System.currentTimeMillis());
	}

	public void emit(boolean emit) {
		if (emit) {
			emit();
		}
	}

	public void emit(long time) {
		if (LooseContext.is(CONTEXT_MUTED)) {
			return;
		}
		String timeStamp = new SimpleDateFormat("HH:mm:ss,SSS")
				.format(new Date(time));
		String key = Ax.format("[alc-%s]", getClass().getCanonicalName());
		DevStats.topicEmitStat
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