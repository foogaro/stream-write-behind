package com.foogaro.redis.wbs.core.orchestrator;

import com.foogaro.redis.wbs.core.exception.AcknowledgeMessageException;
import com.foogaro.redis.wbs.core.exception.ProcessMessageException;
import com.foogaro.redis.wbs.core.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractProcessOrchestrator<T, R> implements ProcessOrchestrator<T, R> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicInteger sequentialPriority = new AtomicInteger(Integer.MAX_VALUE);

    private Processor<T, R> processor;

//    public DefaultProcessOrchestrator() {
//        this.processors = new ArrayList<>();
//    }

//    public DefaultProcessOrchestrator(Processor<T, R> processor) {
//        this.processors = new ArrayList<>();
//        this.processors.add(processor);
//        this.processor = processor;
//    }

//    @Override
//    public void addProcessor(Processor<T, R> processor) {
//        Objects.requireNonNull(processor, "Processor cannot be null");
//        if (processor.getPriority() == 0) {
//            processor.setPriority(sequentialPriority.getAndDecrement());
//        }
//        this.processor = processor;
//        processors.add(processor);
//    }

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
