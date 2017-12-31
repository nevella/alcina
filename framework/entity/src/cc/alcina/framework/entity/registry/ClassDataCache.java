package cc.alcina.framework.entity.registry;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.EncryptionUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class ClassDataCache implements Serializable {
	static final transient long serialVersionUID = -1L;

	public Map<String, ClassDataItem> classData = new LinkedHashMap<String, ClassDataItem>();

	public List<String> ignorePackageSegments = new ArrayList<String>();

	public void add(ClassDataItem item) {
		for (String segment : ignorePackageSegments) {
			if (item.className.startsWith(segment)) {
				return;
			}
		}
		classData.put(item.className, item);
	}

	public static class ClassDataItem implements Serializable {
		static final transient long serialVersionUID = -1L;

		public Date date;

		public String md5;

		public transient URL url;

		public String className;

		public ClassDataItem() {
		}

		public String ensureMd5() {
			if (md5 == null) {
				try {
					if (url == null) {
						md5 = String.valueOf(System.currentTimeMillis());
					} else {
						InputStream stream = url.openStream();
						evalMd5(stream);
					}
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
			return md5;
		}

		public void evalMd5(InputStream stream) {
			try {
				byte[] bytes = ResourceUtilities.readStreamToByteArray(stream);
				md5 = EncryptionUtils.MD5(bytes);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}