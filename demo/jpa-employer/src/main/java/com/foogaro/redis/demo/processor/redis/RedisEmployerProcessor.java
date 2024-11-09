//package com.foogaro.redis.demo.processor.redis;
//
//import com.foogaro.redis.demo.entity.Employer;
//import com.foogaro.redis.demo.repository.redis.RedisEmployerRepository;
//import com.foogaro.redis.wbs.core.processor.AbstractProcessor;
//import com.foogaro.redis.wbs.core.service.BeanFinder;
//import org.springframework.beans.factory.ListableBeanFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Component
//public class RedisEmployerProcessor extends AbstractProcessor<Employer, RedisEmployerRepository> {
//
//    @Autowired
//    private ListableBeanFactory beanFactory;
//
//    private BeanFinder beanFinder;
//
//    @Override
//    public BeanFinder getRepositoryFinder() {
//        if (beanFinder == null) {
//             beanFinder = new BeanFinder(beanFactory);
//        }
//        return beanFinder;
//    }
//
//
//}
