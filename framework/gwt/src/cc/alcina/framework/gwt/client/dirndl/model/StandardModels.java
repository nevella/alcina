package cc.alcina.framework.gwt.client.dirndl.model;

public class StandardModels {
	public static class HeaderContentModel extends Model {
		private Object headerModel;

		private Object contentModel;

		public Object getHeaderModel() {
			return this.headerModel;
		}

		public void setHeaderModel(Object headerModel) {
			Object old_headerModel = this.headerModel;
			this.headerModel = headerModel;
			propertyChangeSupport().firePropertyChange("headerModel",
					old_headerModel, headerModel);
		}

		public Object getContentModel() {
			return this.contentModel;
		}

		public void setContentModel(Object contentModel) {
			Object old_contentModel = this.contentModel;
			this.contentModel = contentModel;
			propertyChangeSupport().firePropertyChange("contentModel",
					old_contentModel, contentModel);
		}
	}
}
