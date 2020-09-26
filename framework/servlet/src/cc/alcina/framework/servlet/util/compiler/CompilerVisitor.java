package cc.alcina.framework.servlet.util.compiler;

import java.net.URL;

import javax.tools.StandardJavaFileManager;

public interface CompilerVisitor {
	public boolean handles(URL url);

	StandardJavaFileManager getFileManager();
}
