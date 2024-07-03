package cc.alcina.extras.dev.codeservice;

import java.net.URLClassLoader;

import cc.alcina.extras.dev.codeservice.CodeService.Context;

/* A disposable classloader, for reloading re-compiled classes */
class DispClassLoader extends URLClassLoader {
	Context context;

	public DispClassLoader(CodeService.Context context, ClassLoader parent) {
		super(context.getClassPathUrls(), parent);
		this.context = context;
	}

	/**
	 * Remove some of the excess locking that we'd normally inherit from
	 * loadClass.
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		boolean resolveWithParent = !context.isInSourcePath(name);
		resolveWithParent |= name
				.startsWith("cc.alcina.extras.dev.console.code");
		resolveWithParent |= name
				.startsWith("cc.alcina.extras.dev.codeservice");
		resolveWithParent |= name
				.startsWith("cc.alcina.framework.common.client.reflection");
		resolveWithParent |= name.startsWith(
				"cc.alcina.framework.common.client.logic.reflection");
		resolveWithParent |= name.startsWith("com.google");
		resolveWithParent |= name.startsWith("org.w3c");
		if (resolveWithParent) {
			return super.loadClass(name, resolve);
		} else {
			Class c = findLoadedClass(name);
			if (c == null) {
				c = findClass(name);
			}
			if (resolve) {
				resolveClass(c);
			}
			return c;
		}
	}
}