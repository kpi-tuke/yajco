package tuke.pargen.annotation.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
//Target: None
public @interface TokenDef {

	String name();

	String regexp();
}
