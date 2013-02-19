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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.logic.EntityLayerLocator;

/**
 * 
 * @author Nick Reddel
 */
public abstract class CachingScanner {
	protected void putIgnoreMap(Map<String, Date> outgoingIgnoreMap,
			String cachePath) {
		try {
			File cacheFile = new File(cachePath);
			cacheFile.getParentFile().mkdirs();
			cacheFile.createNewFile();
			ObjectOutputStream oos = new ObjectOutputStream(
					new BufferedOutputStream(new FileOutputStream(cacheFile)));
			oos.writeObject(outgoingIgnoreMap);
			oos.close();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Date> getIgnoreMap(String cachePath) {
		try {
			ObjectInputStream ois = new ObjectInputStream(
					new BufferedInputStream(new FileInputStream(cachePath)));
			Map<String, Date> value = (Map<String, Date>) ois.readObject();
			ois.close();
			return value;
		} catch (Exception e) {
			return new LinkedHashMap<String, Date>();
		}
	}

	protected File getHomeDir() {
		return EntityLayerLocator.get().getDataFolder();
	}

	public void scan(Map<String, Date> classes, String cachePath)
			throws Exception {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		int cc = 0;
		List<String> ignore = new ArrayList<String>();
		long loadClassnanos = 0;
		boolean debug = false;
		Map<String, Date> incomingIgnoreMap = getIgnoreMap(cachePath);
		Map<String, Date> outgoingIgnoreMap = new LinkedHashMap<String, Date>();
		for (String className : classes.keySet()) {
			Class c = null;
			if (ignore.contains(className)) {
				continue;
			}
			Date modDate = classes.get(className);
			boolean ignored = false;
			if (incomingIgnoreMap.containsKey(className)) {
				if (incomingIgnoreMap.get(className) != null
						&& incomingIgnoreMap.get(className).equals(modDate)) {
					outgoingIgnoreMap.put(className, modDate);
					continue;
				}
			}
			try {
				cc++;
				long nt = System.nanoTime();
				c = classLoader.loadClass(className);
				loadClassnanos += (System.nanoTime() - nt);
				process(c, className, modDate, outgoingIgnoreMap);
			} catch (Error eiie) {
				outgoingIgnoreMap.put(className, modDate);
				continue;
			} catch (Exception e) {
				outgoingIgnoreMap.put(className, modDate);
				continue;
			}
		}
		if (debug) {
			System.out.println(CommonUtils.formatJ(
					"Classes: %s -- loadClass: %s", cc,
					loadClassnanos / 1000 / 1000));
		}
		putIgnoreMap(outgoingIgnoreMap, cachePath);
	}

	protected abstract void process(Class c, String className, Date modDate,
			Map<String, Date> outgoingIgnoreMap);
}
