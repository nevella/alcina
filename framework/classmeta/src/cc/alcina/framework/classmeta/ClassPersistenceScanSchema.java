package cc.alcina.framework.classmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.util.CommonUtils;

@XmlRootElement
public class ClassPersistenceScanSchema {
	public List<String> classPathUrls = new ArrayList<>();

	public String targetPath;

	public String sourceScanPath;

	public String sourceNoScanPath;
	
	public String scanResourcePath;

	public String scanClasspathCachePath;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassPersistenceScanSchema) {
			ClassPersistenceScanSchema o = (ClassPersistenceScanSchema) obj;
			return CommonUtils.equals(classPathUrls, o.classPathUrls,
					targetPath, o.targetPath, sourceScanPath, o.sourceScanPath,
					sourceNoScanPath, o.sourceNoScanPath);
		} else {
			return false;
		}
	}
}
