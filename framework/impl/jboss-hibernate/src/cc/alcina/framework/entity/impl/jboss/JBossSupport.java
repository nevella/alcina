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

import java.net.URL;
import java.util.Date;
import java.util.List;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilter;

import cc.alcina.framework.entity.util.ClasspathScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ClasspathVisitor;


/**
 *
 * @author Nick Reddel
 */

 public class JBossSupport {
	public static void install() {
		ClasspathScanner.installVisitor(VFSClasspathVisitor.class);
	}

	public static class VFSClasspathVisitor extends ClasspathVisitor {
		public VFSClasspathVisitor(ClasspathScanner scanner) {
			super(scanner);
		}

		protected static final Object PROTOCOL_VFS = "vfs";

		protected static final Object PROTOCOL_VFSZIP = "vfszip";

		protected static final Object PROTOCOL_VFSFILE = "vfsfile";

		@Override
		// no ignore-packages, unlike other visitors (unused on the server)
		public void enumerateClasses(URL url) throws Exception {
			VirtualFile root = VFS.getRoot(url);
			List<VirtualFile> children = root
					.getChildrenRecursively(new VirtualFileFilter() {
						public boolean accepts(VirtualFile vf) {
							return vf.getName().endsWith(".class");
						}
					});
			for (VirtualFile file : children) {
				String pathName = file.getPathName().substring(root.getPathName().length()+1);
				if (pathName.endsWith(".class")) {
					String cName = pathName.substring(0, pathName.length() - 6)
							.replace('/', '.');
					scanner.classMap.put(cName,
							new Date(file.getLastModified()));
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
