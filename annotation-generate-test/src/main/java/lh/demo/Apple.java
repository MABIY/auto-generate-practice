package lh.demo;

import com.fasterxml.jackson.annotation.JsonView;
import lh.demo.annotations.AutoValueDto;
import lh.demo.annotations.Views;
import lombok.Data;

/**
 *
 * @author lh
 */
@AutoValueDto
@Data
public class Apple {

    @JsonView(value = Views.NEW.class)
    private int age;

    @JsonView(value = Views.NEW.class)
    private String color;
}
