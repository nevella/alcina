package cc.alcina.framework.gwt.client.gwittir.renderer;

import java.util.Date;

import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.common.client.util.Ax;

@Reflected
public class ShortDateRenderer implements Renderer<Date, String> {
	public String render(Date date) {
		return Ax.dateSlash(date);
	}
}
