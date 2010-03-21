package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;

@SuppressWarnings("all")
public class DisplayInfoImpl implements DisplayInfo {
  private int displayMask;
  public int displayMask(){return displayMask;}
  
  private int orderingHint;
  public int orderingHint(){return orderingHint;}
  
  private String iconName;
  public String iconName(){return iconName;}
  
  private String info;
  public String info(){return info;}
  
  private String name;
  public String name(){return name;}
  
  
  public Class<? extends Annotation> annotationType() {
    return DisplayInfo.class;
  }
  
  public DisplayInfoImpl (int displayMask, int orderingHint, String iconName, String info, String name){
    this.displayMask = displayMask;
    this.orderingHint = orderingHint;
    this.iconName = iconName;
    this.info = info;
    this.name = name;
  }
}
