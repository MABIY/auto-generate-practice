package lh.demo.util;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class APUtils {

  @FunctionalInterface
  public interface GetClassValue {
    void execute() throws MirroredTypeException, MirroredTypesException;
  }

  public static List<? extends TypeMirror> getTypeMirrorFromAnnotationValue(GetClassValue c) {
    try {
      c.execute();
    }
    catch(MirroredTypesException ex) {
      return ex.getTypeMirrors();
    }
    return null;
  }

    public static String getPackageName(TypeElement classElement) {
        return ((PackageElement) classElement.getEnclosingElement())
                .getQualifiedName()
                .toString();
    }

    public static String getSimpleClassName(String qualifiedClassName) {
        return qualifiedClassName.substring(qualifiedClassName.lastIndexOf(".") + 1);
    }
}