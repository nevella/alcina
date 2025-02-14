/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity.impl.jboss;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;
import java.util.logging.Level;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.util.ClasspathScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ClasspathVisitor;
import cc.alcina.framework.entity.util.source.SourceFinder;

/**
 *
 * @author Nick Reddel
 */
public class JBoss7Support {
	protected static final String PROTOCOL_VFS = "vfs";

	protected static final String PROTOCOL_VFSZIP = "vfszip";

	protected static final String PROTOCOL_VFSFILE = "vfsfile";

	public static void install() {
		ClasspathScanner.installVisitor(VFSClasspathVisitor.class);
		SourceFinder.sourceFinders.add(new SourceFinderVfs());
		Registry.register().singleton(
				AppPersistenceBase.InitRegistrySupport.class,
				new InitRegistrySupportImpl());
	}

	public static class InitRegistrySupportImpl
			extends AppPersistenceBase.InitRegistrySupport {
		private Level level;

		@Override
		public void muteClassloaderLogging(boolean mute) {
			java.util.logging.Logger logger = java.util.logging.Logger
					.getLogger("org.jboss.modules");
			if (mute) {
				level = logger.getLevel();
				logger.setLevel(java.util.logging.Level.SEVERE);
			} else {
				logger.setLevel(level);
			}
		}
	}

	static class SourceFinderVfs implements SourceFinder {
		@Override
		public String findSource0(Class clazz) {
			try {
				CodeSource codeSource = clazz.getProtectionDomain()
						.getCodeSource();
				URL classFileLocation = codeSource.getLocation();
				if (classFileLocation.toString().startsWith("vfs:")) {
					VirtualFile root = VFS.getChild(classFileLocation.toURI());
					String childPath = Ax.format("%s.java",
							clazz.getName().replace(".", "/"));
					VirtualFile child = root.getChild(childPath);
					if (child.exists()) {
						return Io.read().fromStream(child.openStream())
								.asString();
					}
				}
				return null;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public File findSourceFile0(Class clazz) {
			throw new UnsupportedOperationException(
					"Unimplemented method 'findSourceFile0'");
		}
	}

	public static class VFSClasspathVisitor extends ClasspathVisitor {
		public VFSClasspathVisitor(ClasspathScanner scanner) {
			super(scanner);
		}

		@Override
		// no ignore-packages, unlike other visitors (unused on the server)
		public void enumerateClasses(URL url) throws Exception {
			VirtualFile root = VFS.getChild(url.toURI());
			List<VirtualFile> children = root
					.getChildrenRecursively(new VirtualFileFilter() {
						@Override
						public boolean accepts(VirtualFile vf) {
							return vf.getName().endsWith(".class");
						}
					});
			for (VirtualFile file : children) {
				String pathName = file.getPathName()
						.substring(root.getPathName().length() + 1);
				if (pathName.endsWith(".class")) {
					add(pathName, file.getLastModified(), file.toURL(), null);
				}
			}
		}

		@Override
		public boolean handles(URL url) {
			return url.getProtocol().equals(PROTOCOL_VFS)
					|| url.getProtocol().equals(PROTOCOL_VFSZIP)
					|| url.getProtocol().equals(PROTOCOL_VFSFILE);
		}
	}
}
