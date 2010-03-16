package cc.alcina.framework.common.client.logic.reflection;

import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import java.lang.annotation.Annotation;

@SuppressWarnings("all")
public class BeanInfoImpl implements BeanInfo {
  private boolean allPropertiesVisualisable;
  public boolean allPropertiesVisualisable(){return allPropertiesVisualisable;}
  
  private DisplayInfo displayInfo;
  public DisplayInfo displayInfo(){return displayInfo;}
  
  private ObjectActions actions;
  public ObjectActions actions(){return actions;}
  
  private Class customizerClass;
  public Class customizerClass(){return customizerClass;}
  
  private String description;
  public String description(){return description;}
  
  private String displayNamePropertyName;
  public String displayNamePropertyName(){return displayNamePropertyName;}
  
  
  public Class<? extends Annotation> annotationType() {
    return BeanInfo.class;
  }
  
  public BeanInfoImpl (boolean allPropertiesVisualisable, DisplayInfo displayInfo, ObjectActions actions, Class customizerClass, String description, String displayNamePropertyName){
    this.allPropertiesVisualisable = allPropertiesVisualisable;
    this.displayInfo = displayInfo;
    this.actions = actions;
    this.customizerClass = customizerClass;
    this.description = description;
    this.displayNamePropertyName = displayNamePropertyName;
  }
}
