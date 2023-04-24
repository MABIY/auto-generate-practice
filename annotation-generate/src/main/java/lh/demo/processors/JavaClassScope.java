package lh.demo.processors;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author lh
 */
@Data
@RequiredArgsConstructor
public final class JavaClassScope {
    private final String packageName;
    private final String sourceClassName;
    private Set<Field> fields = new HashSet<>();

    @Data
    @Builder
    public static class Field{
        private String modifiers;

        private String  type;

        private String name;
    }
}
