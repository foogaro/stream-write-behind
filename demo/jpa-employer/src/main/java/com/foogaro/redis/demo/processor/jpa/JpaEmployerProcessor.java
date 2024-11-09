//package com.foogaro.redis.demo.processor.jpa;
//
//import com.foogaro.redis.demo.entity.Employer;
//import com.foogaro.redis.demo.repository.jpa.JpaEmployerRepository;
//import com.foogaro.redis.wbs.core.processor.AbstractProcessor;
//import com.foogaro.redis.wbs.core.service.BeanFinder;
//import org.springframework.beans.factory.ListableBeanFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Component
//public class JpaEmployerProcessor extends AbstractProcessor<Employer, JpaEmployerRepository> {
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
