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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.registry.RegistryException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.util.ClasspathScanner;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.entity.util.MethodContext;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 *
 * @author Nick Reddel
 *
 *         FIXME - reflection.2 - invalidate on superclass/interface change
 */
public abstract class CachingScanner<T extends ClassMetadata> {
	public static boolean useKryo() {
		return Boolean.getBoolean(
				"cc.alcina.framework.entity.registry.CachingScanner.useKryo");
	}

	int cc = 0;

	long loadClassNanos = 0;

	long loadClassErrNanos = 0;

	boolean debug = false;

	int ignoreCount = 0;

	protected ClassMetadataCache<T> outgoingCache;

	String debugClassloaderExceptionRegex = null;

	Logger logger = LoggerFactory.getLogger(getClass());

	public InputStream getStreamForMd5(ClassMetadata classMetadata)
			throws Exception {
		try {
			return classMetadata.url().openStream();
		} catch (Exception e) {
			// jar changed under us
			return new JarHelper().openStream(classMetadata.url());
		}
	}

	public void scan(ClassMetadataCache<ClassMetadata> classpathCache,
			String cachePath) throws Exception {
		debugClassloaderExceptionRegex = System.getProperty(
				"cc.alcina.framework.entity.registry.CachingScanner.debugClassloaderExceptionRegex");
		List<ClassLoader> classLoaders = ClasspathScanner
				.getScannerClassLoadersToTry();
		File cacheFile = new File(cachePath);
		ClassMetadataCache<T> incomingCache = getCached(cacheFile);
		outgoingCache = new ClassMetadataCache();
		outgoingCache.version = ClassMetadataCache.CURRENT_VERSION;
		long start = System.currentTimeMillis();
		Set<String> invalidated = new ObjectOpenHashSet<>();
		Set<String> invalidatedThisPass = new ObjectOpenHashSet<>();
		Set<String> ensured = new ObjectOpenHashSet<>();
		ClassMetadataCache<T> passIncomingCache = incomingCache;
		int pass = 1;
		int invalidatedParentCount = 0;
		do {
			invalidatedThisPass.clear();
			int loggedThisPass = 0;
			for (ClassMetadata meta : classpathCache.classData.values()) {
				String className = meta.className;
				T out = null;
				T existing = passIncomingCache.classData.get(meta.className);
				boolean unchanged = existing != null
						&& existing.isUnchangedFrom(meta, this);
				boolean invalidatedParent = unchanged
						&& existing.hasInvalidatedParent(invalidated)
						&& !ensured.contains(className);
				if (unchanged && !invalidatedParent) {
					existing.copyMetaFrom(meta);
					out = existing;
				} else {
					if (invalidatedParent) {
						invalidatedParentCount++;
					}
					try {
						cc++;
						Class clazz = loadClass(classLoaders, className);
						out = process(clazz, className, meta);
						out.ensureParents(clazz);
						out.ensureMd5(this);
						invalidatedThisPass.add(className);
						if (passIncomingCache.classData.size() > 0) {
							if (loggedThisPass++ < 10) {
								Ax.out("\t%s", clazz.getName());
							}
						}
						ensured.add(className);
					} catch (RegistryException rre) {
						maybeLog(rre, className);
						throw rre;
					} catch (Error eiie) {
						maybeLog(eiie, className);
						out = createMetadata(className, meta);
						out.invalid = true;
					} catch (ClassNotFoundException
							| TypeNotPresentException e) {
						maybeLog(e, className);
						out = createMetadata(className, meta);
						out.invalid = true;
					} catch (Exception e) {
						maybeLog(e, className);
						throw e;
					}
				}
				outgoingCache.insert(out);
			}
			// only log if theres a delta
			if (!(pass == 1 && invalidatedThisPass.size() == 0)) {
				logger.info(
						"Caching scanner - pass: {} - invalidated: {} - invalidated parents: {}",
						pass, invalidatedThisPass.size(),
						invalidatedParentCount);
			}
			invalidated.addAll(invalidatedThisPass);
			invalidatedParentCount = 0;
			passIncomingCache = outgoingCache;
			pass++;
		} while (invalidatedThisPass.size() > 0);
		long time = System.currentTimeMillis() - start;
		if (debug) {
			System.out.format(
					"Classes: %s -- checked: %s, loadClass: %sms, loadClassErr: %sms, ignoreCount: %s, total: %sms\n",
					classpathCache.classData.size(), cc,
					loadClassNanos / 1000 / 1000,
					loadClassErrNanos / 1000 / 1000, ignoreCount, time);
		}
		serialize(cacheFile);
	}

	private void maybeLog(Throwable throwable, String className) {
		if (Ax.notBlank(debugClassloaderExceptionRegex)
				&& className.matches(debugClassloaderExceptionRegex)) {
			Ax.out("Exception logging caching scanner class resolution: \n\t%s",
					className);
			throwable.printStackTrace();
		}
	}

	private void serialize(File cacheFile) {
		new Thread(Ax.format("caching-scanner-write-%s", cacheFile.getName())) {
			@Override
			public void run() {
				if (useKryo()) {
					KryoUtils.serializeToFile(outgoingCache, cacheFile);
				} else {
					try {
						LooseContext.pushWithTrue(
								JacksonJsonObjectSerializer.CONTEXT_WITHOUT_MAPPER_POOL);
						String out = JacksonUtils.defaultSerializer()
								.withMaxLength(Integer.MAX_VALUE)
								.serialize(outgoingCache);
						ResourceUtilities.write(out, cacheFile);
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						LooseContext.pop();
					}
				}
			};
		}.start();
	}

	protected abstract T createMetadata(String className, ClassMetadata found);

	protected ClassMetadataCache getCached(File cacheFile) {
		ClassMetadataCache cache = MethodContext.instance()
				.withContextTrue(
						JacksonJsonObjectSerializer.CONTEXT_WITHOUT_MAPPER_POOL)
				.withContextClassloader(getClass().getClassLoader())
				.call(() -> {
					try {
						if (useKryo()) {
							return KryoUtils.deserializeFromFile(cacheFile,
									ClassMetadataCache.class);
						} else {
							return JacksonUtils.deserializeFromFile(cacheFile,
									ClassMetadataCache.class);
						}
					} catch (Exception e) {
						if (cacheFile.exists()) {
							cacheFile.delete();
						}
						if (CommonUtils.extractCauseOfClass(e,
								ConnectException.class) != null) {
							logger.warn("ClassMetaServer not reachable");
						} else if (CommonUtils.extractCauseOfClass(e,
								FileNotFoundException.class) != null) {
							logger.info("No cache found, creating");
						}
						return new ClassMetadataCache();
					}
				});
		if (cache.version != ClassMetadataCache.CURRENT_VERSION) {
			cache = new ClassMetadataCache<>();
			cache.version = ClassMetadataCache.CURRENT_VERSION;
		}
		return cache;
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
