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
package cc.alcina.framework.entity.gwt.reflection.jdk;

import java.io.File;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.gwt.reflection.jdk.ValidityScanner.ValidityMetadata;
import cc.alcina.framework.entity.registry.CachingScanner;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.util.ClasspathScanner;

/**
 *
 *
 * @author Nick Reddel
 */
class ValidityScanner extends CachingScanner<ValidityMetadata> {
	@Override
	protected ValidityMetadata createMetadata(String className,
			ClassMetadata found) {
		return new ValidityMetadata(className).fromUrl(found);
	}

	File cacheFile;

	boolean ignoreJars;

	protected Class maybeNormaliseClass(Class c) {
		return c;
	}

	@Override
	protected ValidityMetadata process(Class clazz, String className,
			ClassMetadata found) {
		ValidityMetadata metadata = createMetadata(className, found);
		metadata.generate(clazz);
		return metadata;
	}

	@Override
	public void scan(ClassMetadataCache<ClassMetadata> classDataCache,
			String ignoreClassnameRegex) throws Exception {
		String cachePath = cacheFile.getPath();
		this.ignoreClassnameRegex = ignoreClassnameRegex;
		super.scan(classDataCache, cachePath);
	}

	ClassMetadataCache<ValidityMetadata>
			scan(List<String> classDirectoryPaths) {
		try {
			ClasspathScanner scanner = new ClasspathScanner("*", true,
					ignoreJars);
			for (String path : classDirectoryPaths) {
				scanner.scanDirectory(path);
			}
			ClassMetadataCache<ClassMetadata> dataCache = scanner
					.getClassDataCache();
			scan(dataCache, "__not.*");
			return outgoingCache;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	static class ValidityMetadata extends ClassMetadata<ValidityMetadata> {
		ValidityMetadata() {
		}

		ValidityMetadata(String className) {
			super(className);
		}

		void generate(Class clazz) {
		}
	}
}
