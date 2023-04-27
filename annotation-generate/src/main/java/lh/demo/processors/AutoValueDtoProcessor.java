package lh.demo.processors;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import lh.demo.util.APUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static lh.demo.util.APUtils.*;
import static lh.demo.util.StringUtils.getLastDelimiterValue;
import static lh.demo.util.StringUtils.uppercaseFirstLetter;

/**
 *
 * @author lh
 */
@SupportedAnnotationTypes(
        value = {AutoValueDtoProcessor.LH_DEMO_ANNOTATIONS_AUTO_VALUE_DTO
            //            AutoValueDtoProcessor.COM_FASTERXML_JACKSON_ANNOTATION_JSON_VIEW
        })
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class AutoValueDtoProcessor extends AbstractProcessor {

    public static final String COM_FASTERXML_JACKSON_ANNOTATION_JSON_VIEW = "com.fasterxml.jackson.annotation.JsonView";
    public static final String LH_DEMO_ANNOTATIONS_AUTO_VALUE_DTO = "lh.demo.annotations.AutoValueDto";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeElement autoValueDtoAnnotation =
                processingEnv.getElementUtils().getTypeElement(LH_DEMO_ANNOTATIONS_AUTO_VALUE_DTO);
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(autoValueDtoAnnotation);
        for (Element element : elements) {
            if (element instanceof TypeElement typeElement) {
                String packageName = APUtils.getPackageName(typeElement);
                String classQualifiedName = typeElement.getQualifiedName().toString();

                Map<TypeMirror, Set<VariableElement>> annnotationVariblesPair =
                        processingEnv.getElementUtils().getAllMembers(typeElement).stream()
                                .filter(member -> member.getKind() == ElementKind.FIELD)
                                .filter(member -> member.getAnnotationsByType(JsonView.class).length > 0)
                                .flatMap(member -> APUtils.getAnnotationValuesOrDefault(
                                        member.getAnnotation(JsonView.class)::value, (TypeMirror) processingEnv
                                                .getElementUtils()
                                                .getTypeElement("lh.demo.annotations.Views.Value").asType())
                                        .stream()
                                        .collect(Collectors.toMap(typeMirror -> typeMirror, o -> {
                                            Set<VariableElement> set = new HashSet<>();
                                            set.add((VariableElement) member);
                                            return set;
                                        }))
                                        .entrySet()
                                        .stream())
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (variableElements, variableElements2) -> {
                                            variableElements.addAll(variableElements2);
                                            return variableElements;
                                        }));

                aggregationInterfaceExtendFields(annnotationVariblesPair);
                Map<String,Set<FieldSpec>> typeMirrorSetMap = new HashMap<>();
                for (Map.Entry<TypeMirror, Set<VariableElement>> typeMirrorSetEntry : annnotationVariblesPair.entrySet()) {
                    typeMirrorSetMap.put(classQualifiedName + getLastDelimiterValue(typeMirrorSetEntry.getKey().toString(),'.'),typeMirrorSetEntry.getValue().stream().map(APUtils::convert).collect(Collectors.toSet()));
                }

                for (Map.Entry<String, Set<FieldSpec>> stringSetEntry : typeMirrorSetMap.entrySet()) {

                    String qualifiedClassName = stringSetEntry.getKey();
                    try {
                        String simpleClassName = getLastDelimiterValue(qualifiedClassName,'.');

                        ClassName LombokDateAnnotation = ClassName.get("lombok", "Data");
                        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(simpleClassName)
                                .addAnnotation(LombokDateAnnotation)
                                .addModifiers(Modifier.PUBLIC);
                        for (FieldSpec fieldSpec : stringSetEntry.getValue()) {
                            typeSpecBuilder.addField(fieldSpec);
                        }

                        ClassName  className = ClassName.bestGuess(classQualifiedName);

                        MethodSpec.Builder createMethodBuilder  = MethodSpec.methodBuilder("create")
                                .addModifiers(Modifier.PUBLIC)
                                .returns(className)
                                .addStatement("$T entity = new $T()", className, className);
                        for (FieldSpec fieldSpec : stringSetEntry.getValue()) {
                            extracted(createMethodBuilder, fieldSpec);
                        }
                        createMethodBuilder.addStatement("return entity");
                        typeSpecBuilder.addMethod(createMethodBuilder.build());

                        MethodSpec.Builder assignMethodBuilder  = MethodSpec.methodBuilder("assign")
                                .addModifiers(Modifier.PUBLIC)
                                .returns(className)
                                .addParameter(className,"entity");
                        for (FieldSpec fieldSpec : stringSetEntry.getValue()) {
                            assignMethodBuilder.addStatement("entity.set" + uppercaseFirstLetter(fieldSpec.name) + "(" + fieldSpec.name + ")");
                        }
                        assignMethodBuilder.addStatement("return entity");
                        typeSpecBuilder.addMethod(assignMethodBuilder.build());

                        MethodSpec.Builder patchMethodBuilder  = MethodSpec.methodBuilder("patch")
                                .addModifiers(Modifier.PUBLIC)
                                .returns(className)
                                .addParameter(className,"entity");
                        for (FieldSpec fieldSpec : stringSetEntry.getValue()) {
                            if (fieldSpec.type.isPrimitive()){
                                patchMethodBuilder.addStatement("entity.set" + uppercaseFirstLetter(fieldSpec.name) + "(" + fieldSpec.name + ")");
                            } else  {
                                patchMethodBuilder.beginControlFlow("if ("+ fieldSpec.name+" != null )");
                                patchMethodBuilder.addStatement("entity.set" + uppercaseFirstLetter(fieldSpec.name) + "(" + fieldSpec.name + ")");
                                patchMethodBuilder.endControlFlow();
                            }
                        }
                        patchMethodBuilder.addStatement("return entity");
                        typeSpecBuilder.addMethod(patchMethodBuilder.build());

                        JavaFileObject javaFile = processingEnv.getFiler().createSourceFile(qualifiedClassName);
                        try (PrintWriter out = new PrintWriter(javaFile.openWriter())) {
                            JavaFile.builder(packageName, typeSpecBuilder.build())
                                    .build().writeTo(out);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return true;
    }

    private void aggregationInterfaceExtendFields(Map<TypeMirror, Set<VariableElement>> annotationVariablesPair) {
        Set<TypeMirror>  mirrors  =annotationVariablesPair.keySet();
        for (TypeMirror mirror : mirrors) {
            List<TypeMirror> extendsInterfaces = getAllInterfaces(mirror,processingEnv);
            List<String> typMirrorsString = extendsInterfaces.stream().map(TypeMirror::toString).toList();
            for (TypeMirror typeMirror : mirrors) {
                if(!mirror.toString().equals(typeMirror.toString()) && typMirrorsString.contains(typeMirror.toString())) {
                    annotationVariablesPair.get(mirror).addAll(annotationVariablesPair.get(typeMirror));
                }
            }
        }
    }

    private static void extracted(MethodSpec.Builder createMethodBuilder, FieldSpec fieldSpec) {
        createMethodBuilder.addStatement("entity.set" + uppercaseFirstLetter(fieldSpec.name) + "(" + fieldSpec.name + ")");
    }
}
