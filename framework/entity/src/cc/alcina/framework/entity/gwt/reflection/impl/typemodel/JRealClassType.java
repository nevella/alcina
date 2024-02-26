package cc.alcina.framework.entity.gwt.reflection.impl.typemodel;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.security.CodeSource;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class JRealClassType extends JClassType
		implements com.google.gwt.core.ext.typeinfo.JRealClassType {
	public JRealClassType(TypeOracle typeOracle, Class clazz) {
		super(typeOracle, clazz);
	}

	@Override
	public JClassType getErasedType() {
		return this;
	}

	@Override
	public long getLastModifiedTime() {
		try {
			CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
			if (codeSource == null) {
				return 0L;
			}
			URL url = codeSource.getLocation();
			switch (url.getProtocol()) {
			case "jrt":
			case "jar":
				return 0L;
			}
			File file = Paths.get(url.toURI()).toFile();
			return file.lastModified();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}
