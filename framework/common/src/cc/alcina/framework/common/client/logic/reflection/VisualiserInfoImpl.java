package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;

@SuppressWarnings("all")
public class VisualiserInfoImpl implements VisualiserInfo {
  private DisplayInfo displayInfo;
  public DisplayInfo displayInfo(){return displayInfo;}
  
  private Permission visible;
  public Permission visible(){return visible;}
  
  
  public Class<? extends Annotation> annotationType() {
    return VisualiserInfo.class;
  }
  
  public VisualiserInfoImpl (DisplayInfo displayInfo, Permission visible){
    this.displayInfo = displayInfo;
    this.visible = visible;
  }
}
