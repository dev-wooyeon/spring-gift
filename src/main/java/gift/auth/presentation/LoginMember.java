package gift.auth.presentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to bind the authenticated Member domain object to a controller method parameter.
 *
 * @author brian.kim
 * @since 1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {
    /**
     * Whether the login member is required.
     * If true, an UnauthenticatedException is thrown when no valid member is resolved.
     */
    boolean required() default true;
}
