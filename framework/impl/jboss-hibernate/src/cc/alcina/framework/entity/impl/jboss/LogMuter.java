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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Nick Reddel
 */

 public class LogMuter {
	private String[] info = {
			"org.jboss.seam",
			"org.codehaus.xfire",
			"org.jboss.ejb",
			"org.ajax4jsf",
			"org.jboss.ejb3.entity.ExtendedPersistenceContextPropagationInterceptor",
			"org.hibernate",
			"org.apache.commons.httpclient.HttpMethodDirector",
			"httpclient.wire.content",
			 "org.jboss.resource.connectionmanager.IdleRemover",
			"com.arjuna", "com.arjuna.ats" ,"org.jboss.ejb3.entity.ManagedEntityManagerFactory"};
	private String[] warn={"org.apache.catalina.loader.WebappClassLoader"};

	public void run() {
		new Thread(){
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for (String l : info) {
					Logger.getLogger(l).setLevel(Level.INFO);
				}
				for (String l : warn) {
					Logger.getLogger(l).setLevel(Level.WARN);
				}
			}
		}.start();
	}
}
