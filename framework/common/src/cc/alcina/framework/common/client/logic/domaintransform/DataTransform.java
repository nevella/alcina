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

package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nick Reddel
 */

 public class DataTransform {
	public interface DataTransformListener {
		public void dataTransform(DataTransformEvent evt)
				throws DataTransformException;
	}

	public static class DataTransformException extends Exception implements
			Serializable {
		private DataTransformEvent event;

		public DataTransformEvent getEvent() {
			return this.event;
		}

		public void setEvent(DataTransformEvent event) {
			this.event = event;
		}

		public DataTransformRequest getRequest() {
			return this.request;
		}

		public void setRequest(DataTransformRequest request) {
			this.request = request;
		}

		private DataTransformRequest request;

		public DataTransformException() {
		}

		public DataTransformException(String message, Throwable cause) {
			super(message, cause);
		}

		public DataTransformException(String message) {
			super(message);
		}

		public DataTransformException(Throwable t) {
			super(t);
		}
	}

	public static class DataTransformRuntimeException extends RuntimeException
			implements Serializable {
		private DataTransformEvent event;

		public DataTransformRuntimeException(String message) {
			super(message);
		}

		public void setEvent(DataTransformEvent event) {
			this.event = event;
		}

		public DataTransformEvent getEvent() {
			return event;
		}
	}

	public static class DataTransformSupport {
		private List<DataTransformListener> listenerList = new ArrayList<DataTransformListener>();;

		public void addDataTransformListener(DataTransformListener listener) {
			listenerList.add(listener);
		}

		public void clear() {
			listenerList.clear();
		}

		public void fireDataTransform(DataTransformEvent event)
				throws DataTransformException {
			for (DataTransformListener listener : listenerList) {
				listener.dataTransform(event);
			}
		}

		public void removeDataTransformListener(DataTransformListener listener) {
			listenerList.remove(listener);
		}
	}
}
