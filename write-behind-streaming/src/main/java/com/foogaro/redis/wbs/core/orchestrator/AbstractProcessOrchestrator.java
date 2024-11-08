package com.foogaro.redis.wbs.core.orchestrator;

import com.foogaro.redis.wbs.core.exception.AcknowledgeMessageException;
import com.foogaro.redis.wbs.core.exception.ProcessMessageException;
import com.foogaro.redis.wbs.core.processor.Processor;
import org.springframework.data.redis.connection.stream.MapRecord;

public abstract class AbstractProcessOrchestrator<T, R> implements ProcessOrchestrator<T, R> {

    @Override
    public void orchestrate(MapRecord<String, String, String> record, Processor<T, R> processor) {
        try {
            processor.process(record);
            processor.acknowledge(record);
        } catch (ProcessMessageException e) {
            throw new RuntimeException(e);
        } catch (AcknowledgeMessageException e) {
            throw new RuntimeException(e);
        }
    }

}
