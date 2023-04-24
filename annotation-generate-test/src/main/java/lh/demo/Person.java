package lh.demo;

import com.fasterxml.jackson.annotation.JsonView;
import lh.demo.annotations.AutoValueDto;
import lh.demo.annotations.Views;

/**
 *
 * @author lh
 */
@AutoValueDto
public class Person {

    @JsonView(value = Views.NEW.class)
    private int age;
    private String name;

    public int getAge() {
        return age;
    }

    public Person setAge(int age) {
        this.age = age;
        return this;
    }

    public String getName() {
        return name;
    }

    public Person setName(String name) {
        this.name = name;
        return this;
    }
}
