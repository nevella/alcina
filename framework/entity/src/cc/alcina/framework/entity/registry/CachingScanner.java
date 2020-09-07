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
package cc.alcina.framework.entity.registry;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.registry.RegistryException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.util.ClasspathScanner;

/**
 *
 * @author Nick Reddel
 */
public abstract class CachingScanner<T extends ClassMetadata> {
	int cc = 0;

	long loadClassNanos = 0;

	long loadClassErrNanos = 0;

	boolean debug = false;

	int ignoreCount = 0;

	protected ClassMetadataCache<T> outgoingCache;

	public InputStream getStreamForMd5(ClassMetadata classMetadata)
			throws Exception {
		try {
			return classMetadata.url().openStream();
		} catch (Exception e) {
			// jar changed under us
			return new JarHelper().openStream(classMetadata.url());
		}
	}

	String debugClassloaderExceptionRegex = null;

	public void scan(ClassMetadataCache<ClassMetadata> foundCache,
			String cachePath) throws Exception {
		debugClassloaderExceptionRegex = System.getProperty(
				"cc.alcina.framework.entity.registry.CachingScanner.debugClassloaderExceptionRegex");
		List<ClassLoader> classLoaders = ClasspathScanner
				.getScannerClassLoadersToTry();
		File cacheFile = new File(cachePath);
		ClassMetadataCache<T> incomingCache = getCached(cacheFile);
		outgoingCache = new ClassMetadataCache();
		long start = System.currentTimeMillis();
		for (ClassMetadata found : foundCache.classData.values()) {
			String className = found.className;
			T out = null;
			T existing = incomingCache.classData.get(found.className);
			if (existing != null && existing.isUnchangedFrom(found, this)) {
				existing.copyMetaFrom(found);
				out = existing;
			} else {
				try {
					cc++;
					Class clazz = loadClass(classLoaders, className);
					out = process(clazz, className, found);
					out.ensureMd5(this);
				} catch (RegistryException rre) {
					maybeLog(rre, className);
					throw rre;
				} catch (Error eiie) {
					maybeLog(eiie, className);
					out = createMetadata(className, found);
					out.invalid = true;
				} catch (ClassNotFoundException | TypeNotPresentException e) {
					maybeLog(e, className);
					out = createMetadata(className, found);
					out.invalid = true;
				} catch (Exception e) {
					maybeLog(e, className);
					throw e;
				}
			}
			outgoingCache.add(out);
		}
		long time = System.currentTimeMillis() - start;
		if (debug) {
			System.out.format(
					"Classes: %s -- checked: %s, loadClass: %sms, loadClassErr: %sms, ignoreCount: %s, total: %sms\n",
					foundCache.classData.size(), cc,
					loadClassNanos / 1000 / 1000,
					loadClassErrNanos / 1000 / 1000, ignoreCount, time);
		}
		try {
			LooseContext.pushWithTrue(
					KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER);
			KryoUtils.serializeToFile(outgoingCache, cacheFile);
		} finally {
			LooseContext.pop();
		}
	}

	private void maybeLog(Throwable throwable, String className) {
		if (Ax.notBlank(debugClassloaderExceptionRegex)
				&& className.matches(debugClassloaderExceptionRegex)) {
			Ax.out("Exception logging caching scanner class resolution: \n\t%s",
					className);
			throwable.printStackTrace();
		}
	}

	protected abstract T createMetadata(String className, ClassMetadata found);

	protected ClassMetadataCache getCached(File cacheFile) {
		try {
			LooseContext.pushWithTrue(
					KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER);
			ClassMetadataCache value = KryoUtils.deserializeFromFile(cacheFile,
					ClassMetadataCache.class);
			return value;
		} catch (Exception e) {
			return new ClassMetadataCache();
		} finally {
			LooseContext.pop();
		}
	}

	protected File getHomeDir() {
		return EntityLayerObjects.get().getDataFolder();
	}

	protected Class loadClass(List<ClassLoader> classLoaders, String className)
			throws ClassNotFoundException, Error {
		Class c = null;
		for (int idx = 0; idx < classLoaders.size(); idx++) {
			long nt = System.nanoTime();
			ClassLoader classLoader = classLoaders.get(idx);
			try {
				c = classLoader.loadClass(className);
				loadClassNanos += (System.nanoTime() - nt);
				break;
			} catch (ClassNotFoundException e) {
				loadClassErrNanos += (System.nanoTime() - nt);
				if (idx < classLoaders.size() - 1) {
					continue;
				} else {
					throw e;
				}
			} catch (Error eiie) {
				loadClassErrNanos += (System.nanoTime() - nt);
				if (idx < classLoaders.size() - 1) {
					continue;
				} else {
					throw eiie;
				}
			}
		}
		return c;
	}

	protected abstract T process(Class clazz, String className,
			ClassMetadata found);
}
