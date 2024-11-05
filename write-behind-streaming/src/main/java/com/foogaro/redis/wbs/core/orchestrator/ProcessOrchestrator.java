package com.foogaro.redis.wbs.core.orchestrator;

import com.foogaro.redis.wbs.core.processor.Processor;
import org.springframework.data.redis.connection.stream.MapRecord;

public interface ProcessOrchestrator<T, R> {

//    void addProcessor(Processor<T, R> processor);
//    void orchestrate(MapRecord<String, String, String> record);
    void orchestrate(MapRecord<String, String, String> record, Processor<T, R> processor);

}
