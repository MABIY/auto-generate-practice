package lh.demo.util;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    public static List<? extends TypeMirror> getAnnotationValuesOrDefault(GetClassValue c, TypeMirror defaTypeMirror) {
        List<? extends TypeMirror> mirrors = getTypeMirrorFromAnnotationValue(c);
        if (Objects.isNull(mirrors) || mirrors.isEmpty()) {
            return Collections.singletonList(defaTypeMirror);
        }
        return mirrors;
    }

    public static String getPackageName(TypeElement classElement) {
        return ((PackageElement) classElement.getEnclosingElement())
                .getQualifiedName()
                .toString();
    }

    public static String getSimpleClassName(String qualifiedClassName) {
        return qualifiedClassName.substring(qualifiedClassName.lastIndexOf(".") + 1);
    }

    public static FieldSpec convert(VariableElement fieldElement) {
        return FieldSpec.builder(
                        TypeName.get(fieldElement.asType()),
                        fieldElement.getSimpleName().toString(),
                        fieldElement.getModifiers().toArray(Modifier[]::new))
                .addAnnotations(fieldElement.getAnnotationMirrors().stream()
                        .map(AnnotationSpec::get)
                        .collect(Collectors.toSet()))
                .build();
    }

    public static List<TypeMirror> getAllInterfaces(TypeMirror mirror, ProcessingEnvironment processingEnv) {
        @SuppressWarnings("unchecked")
        List<TypeMirror> interfaceTypes  = (List<TypeMirror>) processingEnv.getElementUtils().getTypeElement(mirror.toString()).getInterfaces();
        if(Objects.isNull(interfaceTypes) ||  interfaceTypes.isEmpty()){
            return interfaceTypes;
        }

        List<TypeMirror> mirrors = new ArrayList<>(interfaceTypes);
        for (TypeMirror typeMirror : interfaceTypes) {
            mirrors.addAll(getAllInterfaces(typeMirror,processingEnv));
        }
        return mirrors;
    }

}