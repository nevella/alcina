package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import cc.alcina.framework.common.client.logic.reflection.Action;

@SuppressWarnings("all")
public class ActionImpl implements Action {
  private Permission permission;
  public Permission permission(){return permission;}
  
  private Class actionClass;
  public Class actionClass(){return actionClass;}
  
  
  public Class<? extends Annotation> annotationType() {
    return Action.class;
  }
  
  public ActionImpl (Permission permission, Class actionClass){
    this.permission = permission;
    this.actionClass = actionClass;
  }
}
