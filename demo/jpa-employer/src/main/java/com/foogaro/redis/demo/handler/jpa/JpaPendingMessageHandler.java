//package com.foogaro.redis.demo.handler.jpa;
//
//import com.foogaro.redis.demo.entity.Employer;
//import com.foogaro.redis.demo.processor.jpa.JpaEmployerProcessor;
//import com.foogaro.redis.demo.repository.jpa.JpaEmployerRepository;
//import com.foogaro.redis.wbs.core.handler.AbstractPendingMessageHandler;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Component
//public class JpaPendingMessageHandler extends AbstractPendingMessageHandler<Employer, JpaEmployerRepository> {
//
//    @Autowired
//    private JpaEmployerProcessor processor;
//
//    @Override
//    public JpaEmployerProcessor getProcessor() {
//        return processor;
//    }
//}
