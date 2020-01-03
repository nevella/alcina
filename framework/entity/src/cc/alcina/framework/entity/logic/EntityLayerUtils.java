package cc.alcina.framework.entity.logic;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;

public class EntityLayerUtils {

	public static String getLocalHostName() {
		try {
			String defined = ResourceUtilities.get(EntityLayerUtils.class,
					"localHostName");
			if (Ax.isBlank(defined)) {
				return java.net.InetAddress.getLocalHost().getHostName();
			} else {
				return defined;
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
	public static String getApplicationHostName() {
		return ResourceUtilities.get("applicationHostName");
	}
}
