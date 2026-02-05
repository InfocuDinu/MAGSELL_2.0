package com.bakerymanager.config;

import javafx.util.Callback;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SpringFXMLLoader implements Callback<Class<?>, Object> {
    
    private final ApplicationContext applicationContext;
    
    public SpringFXMLLoader(@Lazy ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public Object call(Class<?> controllerClass) {
        return applicationContext.getBean(controllerClass);
    }
}
