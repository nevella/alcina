package cc.alcina.framework.gwt.client.gwittir.renderer;

import java.util.Date;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;

@Reflected
public class ShortDateRenderer implements Function<Date, String> {
	public String apply(Date date) {
		return Ax.dateSlash(date);
	}
}
