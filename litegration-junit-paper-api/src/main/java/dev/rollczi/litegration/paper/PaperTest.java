package dev.rollczi.litegration.paper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.platform.commons.annotation.Testable;

/**
 * The PaperTest annotation is used to mark test methods or test classes that are specific to
 * Paper. The annotated elements can be discovered and executed by the Litegration JUnit Paper engine
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Testable
public @interface PaperTest {
}