package lh.demo.processors;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.auto.service.AutoService;
import lh.demo.util.APUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static lh.demo.util.APUtils.getSimpleClassName;

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

                //                String simpleClassName = typeElement.getSimpleName().toString();
                List<Element> fieldsWithJsonViewAnnotation =
                        processingEnv.getElementUtils().getAllMembers(typeElement).stream()
                                .filter(element1 -> element1.getKind() == ElementKind.FIELD)
                                .filter(element1 -> element1.getAnnotationsByType(JsonView.class).length > 0)
                                .collect(Collectors.toList());

                Map<String, Set<JavaClassScope.Field>> classFieldsPair = new HashMap<>();
                fieldsWithJsonViewAnnotation.forEach(fieldElement -> {
                    APUtils.getTypeMirrorFromAnnotationValue(fieldElement.getAnnotation(JsonView.class)::value)
                            .forEach(typeMirror -> {
                                String name = typeMirror
                                        .toString()
                                        .substring(typeMirror.toString().lastIndexOf('.') + 1);
                                String key = classQualifiedName + name;
                                Set<JavaClassScope.Field> fields = classFieldsPair.getOrDefault(key, new HashSet<>());
                                fields.add(JavaClassScope.Field.builder()
                                        .modifiers(fieldElement.getModifiers().stream()
                                                .map(Modifier::toString)
                                                .collect(Collectors.joining(" ")))
                                        .type(fieldElement.asType().toString())
                                        .name(fieldElement.getSimpleName().toString())
                                        .build());
                                classFieldsPair.put(key, fields);
                            });
                });


                for (Map.Entry<String, Set<JavaClassScope.Field>> stringSetEntry : classFieldsPair.entrySet()) {

                    String qualifiedClassName = stringSetEntry.getKey();
                    JavaClassScope classScope = new JavaClassScope(packageName, getSimpleClassName(qualifiedClassName));
                    classScope.setFields(stringSetEntry.getValue());


                    MustacheFactory factory = new DefaultMustacheFactory();
                    Mustache mustache = factory.compile("template/JavaClass.mustache");
                    try {
                        JavaFileObject javaFile = processingEnv.getFiler().createSourceFile(qualifiedClassName);
                        try (PrintWriter out = new PrintWriter(javaFile.openWriter())) {
                            mustache.execute(out, classScope);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return true;
    }
}
