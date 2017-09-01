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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryException;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.registry.ClassDataCache.ClassDataItem;
import cc.alcina.framework.entity.util.ClasspathScanner;

/**
 *
 * @author Nick Reddel
 */
public abstract class CachingScanner {
	protected void putIgnoreMap(ClassDataCache dataCache, String cachePath) {
		try {
			File cacheFile = new File(cachePath);
			cacheFile.getParentFile().mkdirs();
			cacheFile.createNewFile();
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)));
			oos.writeObject(dataCache);
			oos.close();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected ClassDataCache getIgnoreMap(String cachePath) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cachePath)));
			ClassDataCache value = (ClassDataCache) ois.readObject();
			ois.close();
			return value;
		} catch (Exception e) {
			return new ClassDataCache();
		}
	}

	protected File getHomeDir() {
		return EntityLayerObjects.get().getDataFolder();
	}

	public void scan(ClassDataCache found, String cachePath) throws Exception {
		List<ClassLoader> classLoaders = ClasspathScanner.getScannerClassLoadersToTry();
		int cc = 0;
		long loadClassNanos = 0;
		long loadClassErrNanos = 0;
		boolean debug = false;
		int ignoreCount = 0;
		ClassDataCache ignoreCache = getIgnoreMap(cachePath);
		ClassDataCache outgoing = new ClassDataCache();
		long start = System.currentTimeMillis();
		for (ClassDataItem foundItem : found.classData.values()) {
			String className = foundItem.className;
			if(className.contains("DefaultViewActionHandler3")){
	            int debug2=3;
	        }
			Class c = null;
			ClassDataItem ignore = ignoreCache.classData.get(foundItem.className);
			if (ignore != null) {
				if (ignore.date.getTime() >= foundItem.date.getTime()) {
					outgoing.add(ignore);
					ignoreCount++;
					continue;
				}
				if (ignore.md5 != null && ignore.md5.equals(foundItem.ensureMd5())) {
					outgoing.add(foundItem);
					ignoreCount++;
					continue;
				}
			}
			try {
				cc++;
				int idx = 0;
				for (int i = 0; i < classLoaders.size(); i++) {
					long nt = System.nanoTime();
					ClassLoader classLoader = classLoaders.get(i);
					try {
						c = classLoader.loadClass(className);
						loadClassNanos += (System.nanoTime() - nt);
						break;
					} catch (ClassNotFoundException e) {
						loadClassErrNanos += (System.nanoTime() - nt);
						if (i < classLoaders.size() - 1) {
							continue;
						} else {
							throw e;
						}
					} catch (Error eiie) {
						loadClassErrNanos += (System.nanoTime() - nt);
						if (i < classLoaders.size() - 1) {
							continue;
						} else {
							throw eiie;
						}
					}
				}
				process(c, className, foundItem, outgoing);
			} catch (RegistryException rre) {
				throw rre;
			} catch (Error eiie) {
				outgoing.add(foundItem);
				continue;
			} catch (ClassNotFoundException e) {
				outgoing.add(foundItem);
				continue;
			} catch (Exception e) {
				throw e;
			}
		}
		long time = System.currentTimeMillis() - start;
		if (debug) {
			System.out.format(
					"Classes: %s -- checked: %s, loadClass: %sms, loadClassErr: %sms, ignoreCount: %s, total: %sms\n",
					found.classData.size(), cc, loadClassNanos / 1000 / 1000, loadClassErrNanos / 1000 / 1000,
					ignoreCount, time);
		}
		for (ClassDataItem item : outgoing.classData.values()) {
			item.ensureMd5();
		}
		if (found.classData.values().size() > 0) {
			putIgnoreMap(outgoing, cachePath);
		}
	}

	protected abstract void process(Class c, String className, ClassDataItem foundItem, ClassDataCache outgoing);
}
