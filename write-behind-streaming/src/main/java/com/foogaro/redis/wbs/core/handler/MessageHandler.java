package com.foogaro.redis.wbs.core.handler;

import com.foogaro.redis.wbs.core.processor.Processor;

public interface MessageHandler<T, R> {

    Processor<T, R> getProcessor();

}
