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

import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.classmeta.ClasspathUrlTranslator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.mvcc.SourceFinder;
import cc.alcina.framework.entity.util.ClasspathScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ClasspathVisitor;

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
		CachingClasspathScanner
				.installUrlTranslator(new VFSClasspathUrlTranslator());
		SourceFinder.sourceFinders.add(new SourceFinderVfs());
		// ServerCodeCompiler.install(new VFSFileManager());
	}

	public static class VFSClasspathUrlTranslator
			implements ClasspathUrlTranslator {
		@Override
		public URL translateClasspathUrl(URL in) {
			try {
				switch (in.getProtocol()) {
				case PROTOCOL_VFS:
					return new URI(in.toString().replaceFirst("vfs:", "file:"))
							.toURL();
				case PROTOCOL_VFSFILE:
					throw new UnsupportedOperationException();
				}
				return in;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
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

	static class SourceFinderVfs implements SourceFinder {
		@Override
		public String findSource(Class clazz) {
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
						return ResourceUtilities
								.readStreamToString(child.openStream());
					}
				}
				return null;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}
