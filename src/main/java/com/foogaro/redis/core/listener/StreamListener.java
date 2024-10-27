package com.foogaro.redis.core.listener;

import org.springframework.data.redis.connection.stream.MapRecord;

public interface StreamListener {

    void onMessage(MapRecord<String, String, String> message);

}
