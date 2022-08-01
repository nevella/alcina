package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class LeafModel {
	@Directed(bindings = @Binding(from = "src", type = Type.PROPERTY))
	public static class Img extends Model {
		private String src;

		public Img() {
		}

		public Img(String src) {
			this.src = src;
		}

		public String getSrc() {
			return this.src;
		}

		public void setSrc(String src) {
			String old_src = this.src;
			this.src = src;
			propertyChangeSupport().firePropertyChange("src", old_src, src);
		}
	}
}
