package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.csobjects.BaseBindable;

/*
 * Contains either a standard layout model (such as HeaderContent) (for normal UI) or a model which is essentially a modal in-page
 * 
 * Should render by delegating to the model field
 */
public class TopModel extends BaseBindable {
    private Object model;

    public Object getModel() {
        return this.model;
    }

    public void setModel(Object model) {
        Object old_model = this.model;
        this.model = model;
        propertyChangeSupport().firePropertyChange("model", old_model, model);
    }
}
