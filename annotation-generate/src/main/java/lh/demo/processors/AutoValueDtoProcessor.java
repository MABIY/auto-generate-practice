package lh.demo.processors;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.auto.service.AutoService;
import lh.demo.util.APUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author lh
 */
@SupportedAnnotationTypes(
        value = {
            AutoValueDtoProcessor.LH_DEMO_ANNOTATIONS_AUTO_VALUE_DTO,
            AutoValueDtoProcessor.COM_FASTERXML_JACKSON_ANNOTATION_JSON_VIEW
        })
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class AutoValueDtoProcessor extends AbstractProcessor {

    public static final String COM_FASTERXML_JACKSON_ANNOTATION_JSON_VIEW = "com.fasterxml.jackson.annotation.JsonView";
    public static final String LH_DEMO_ANNOTATIONS_AUTO_VALUE_DTO = "lh.demo.annotations.AutoValueDto";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Optional<? extends TypeElement> autoValueDto = annotations.stream()
                .filter(typeElement -> typeElement.toString().equals(LH_DEMO_ANNOTATIONS_AUTO_VALUE_DTO))
                .findFirst();
        Optional<? extends TypeElement> jsonView = annotations.stream()
                .filter(typeElement -> typeElement.toString().equals(COM_FASTERXML_JACKSON_ANNOTATION_JSON_VIEW))
                .findFirst();
        autoValueDto.ifPresent(typeElement -> {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(typeElement);
            Map<String, List<Element>> classElementPair = elements.stream()
                    .collect(Collectors.groupingBy(element ->
                            ((TypeElement) element).getQualifiedName().toString()));
            Map<String, List<Element>> jsonViewClassElementPair = new HashMap<>();
            if (jsonView.isPresent()) {
                Set<? extends Element> jsonViewElements = roundEnv.getElementsAnnotatedWith(jsonView.get());
                jsonViewClassElementPair = jsonViewElements.stream()
                        .collect(Collectors.groupingBy(element -> ((TypeElement) element.getEnclosingElement())
                                .getQualifiedName()
                                .toString()));
            }

            Set<String> qualifiedClassSet = classElementPair.keySet();
            qualifiedClassSet.retainAll(jsonViewClassElementPair.keySet());

            Map<String, List<Element>> classAndField = new HashMap<>();
            for (String qualifiedClass : qualifiedClassSet) {
                jsonViewClassElementPair.get(qualifiedClass).forEach(element -> {
                     APUtils.getTypeMirrorFromAnnotationValue(element.getAnnotation(JsonView.class)::value).stream().forEach(typeMirror -> {
                         String name = typeMirror.toString().substring(typeMirror.toString().lastIndexOf('.')+1);
                         String key = qualifiedClass + name;
                         List<Element> fields = classAndField.getOrDefault(key, new ArrayList<>());
                         fields.add(element);
                         classAndField.put(key, fields);
                     });
                });
            }

            writeDto(classAndField);

        });

        return true;
    }

    private void writeDto(Map<String,List<Element>> classFieldsPair) {
        classFieldsPair.forEach((classQualifiedName, fields) -> {
            String packageName = null;
            int lastDot = classQualifiedName.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = classQualifiedName.substring(0, lastDot);
            }

            String simpleClassName = classQualifiedName.substring(lastDot + 1);

            try {
                JavaFileObject javaFile = processingEnv.getFiler()
                        .createSourceFile(classQualifiedName);

                try(PrintWriter out = new PrintWriter(javaFile.openWriter())){
                    if(packageName !=null) {
                        out.print("package ");
                        out.print(packageName);
                        out.println(";");
                        out.println();
                    }

                    //impoort
                    out.println("import lombok.Data;");
                    out.println();

                    out.println("@Data");
                    out.print("public class ");
                    out.print(simpleClassName);
                    out.print(" {");
                    out.println();
                    fields.forEach(s -> {
                        out.print("  " + s.getModifiers().stream().map(Modifier::toString).collect(Collectors.joining(" ")));
                        out.print(" " + s.asType().toString());
                        out.println(" " + s +";");

                    });
                    out.println();
                    out.print("}");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }
}
