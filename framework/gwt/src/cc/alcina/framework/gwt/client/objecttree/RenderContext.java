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
package cc.alcina.framework.gwt.client.objecttree;

import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.util.Callback;

import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Nick Reddel
 */
public class RenderContext {
	public Map<String, Object> properties = new HashMap<String, Object>();

	private static RenderContext current;

	/**
	 * this needs some work. "current" should only be available at time of
	 * rendering - and null-ed thereafter components which may need it should
	 * take a reference at render time
	 */
	public static RenderContext current() {
		return current;
	}

	public static void setCurrent(RenderContext current) {
		RenderContext.current = current;
	}

	private TreeRenderer rootRenderer;

	private IsRenderableFilter renderableFilter = null;

	private Callback<Widget> onAttachCallback;

	private Callback<Widget> onDetachCallback;

	public <T> T get(String key) {
		return (T) properties.get(key);
	}

	public Integer getInteger(String key) {
		return (Integer) properties.get(key);
	}

	public Callback<Widget> getOnAttachCallback() {
		return this.onAttachCallback;
	}

	public Callback<Widget> getOnDetachCallback() {
		return this.onDetachCallback;
	}

	public IsRenderableFilter getRenderableFilter() {
		return renderableFilter;
	}

	public TreeRenderer getRootRenderer() {
		return this.rootRenderer;
	}

	public String getString(String key) {
		return (String) properties.get(key);
	}

	public boolean isBoolean(String key) {
		return properties.get(key) == Boolean.TRUE;
	}

	public void onAttach(Widget widget) {
		if (onAttachCallback != null) {
			onAttachCallback.callback(widget);
		}
	}

	public void onDetach(Widget widget) {
		if (onDetachCallback != null) {
			onDetachCallback.callback(widget);
		}
	}

	public void set(String key, Object value) {
		properties.put(key, value);
	}

	public void setBoolean(String key) {
		properties.put(key, Boolean.TRUE);
	}

	public void setOnAttachCallback(Callback<Widget> onAttachCallback) {
		this.onAttachCallback = onAttachCallback;
	}

	public void setOnDetachCallback(Callback<Widget> onDetachCallback) {
		this.onDetachCallback = onDetachCallback;
	}

	public void setRenderableFilter(IsRenderableFilter renderableFilter) {
		this.renderableFilter = renderableFilter;
	}

	public void setRootRenderer(TreeRenderer rootRenderer) {
		this.rootRenderer = rootRenderer;
	}
}
