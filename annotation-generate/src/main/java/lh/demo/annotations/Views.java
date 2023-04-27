package lh.demo.annotations;

/**
 * @author lh
 */
public class Views {
    public interface NEW extends Value{};
    public interface Value extends Test {};

    public interface Test{}
}
