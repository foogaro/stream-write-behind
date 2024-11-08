package com.foogaro.redis.wbs.core.processor;

import com.foogaro.redis.wbs.core.service.BeanFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessorGenerator {

    @Autowired
    private final BeanFinder beanFinder;

    public ProcessorGenerator(BeanFinder beanFinder) {
        this.beanFinder = beanFinder;
    }


}
