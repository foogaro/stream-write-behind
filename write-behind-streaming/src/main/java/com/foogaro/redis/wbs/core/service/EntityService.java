package com.foogaro.redis.wbs.core.service;

import com.foogaro.redis.wbs.core.Misc;

import java.lang.reflect.ParameterizedType;

public class EntityService<R> {

    private final Class<R> entityClass;

    @SuppressWarnings("unchecked")
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
