package com.foogaro.redis.core.service;

import com.foogaro.redis.core.Misc;

import java.lang.reflect.ParameterizedType;

public class EntityService<R> {

    private final Class<R> entityClass;

    public EntityService() {
        this.entityClass = (Class<R>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public Class<R> getEntityClass() {
        return entityClass;
    }

    protected String getStreamKey() {
        return Misc.getStreamKey(entityClass);
    }

}
