package cc.alcina.framework.common.client.logic.reflection;

import cc.alcina.framework.common.client.logic.reflection.ObjectActions;
import java.lang.annotation.Annotation;

@SuppressWarnings("all")
public class ObjectActionsImpl implements ObjectActions {
  private Action[] value;
  public Action[] value(){return value;}
  
  
  public Class<? extends Annotation> annotationType() {
    return ObjectActions.class;
  }
  
  public ObjectActionsImpl (Action[] value){
    this.value = value;
  }
}
