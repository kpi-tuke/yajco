package yajco.annotation.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
//Target: None
public @interface Skip {
    String value();
}
