package ecommerce.common.constant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    
    String headerName() default "Idempotency-Key";
    
    boolean validatePayload() default true;
    
    String errorMessage() default "Idempotency key already used with different payload";
}