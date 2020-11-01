@Target(AnnotationTarget.LOCAL_VARIABLE)
@Retention(RetentionPolicy.BINARY)
public @interface Source {}

@Target({AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.METHOD})
@Retention(RetentionPolicy.BINARY)
public @interface Sink {}
