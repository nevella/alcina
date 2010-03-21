package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import cc.alcina.framework.common.client.logic.reflection.WrapperInfo;

@SuppressWarnings("all")
public class WrapperInfoImpl implements WrapperInfo {
  private boolean cascadeDelete;
  public boolean cascadeDelete(){return cascadeDelete;}
  
  private String idPropertyName;
  public String idPropertyName(){return idPropertyName;}
  
  private String toStringPropertyName;
  public String toStringPropertyName(){return toStringPropertyName;}
  
  
  public Class<? extends Annotation> annotationType() {
    return WrapperInfo.class;
  }
  
  public WrapperInfoImpl (boolean cascadeDelete, String idPropertyName, String toStringPropertyName){
    this.cascadeDelete = cascadeDelete;
    this.idPropertyName = idPropertyName;
    this.toStringPropertyName = toStringPropertyName;
  }
}
