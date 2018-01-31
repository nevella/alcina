package cc.alcina.framework.classmeta;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;

public class ClassMetaRequest {
	public ClassMetaRequestType type;
	List<URL> classPaths = new ArrayList<>();
	@Override
	public String toString() {
		return Ax.format("Type: %s - classPaths: %s", type, classPaths.size());
	}
	
}
