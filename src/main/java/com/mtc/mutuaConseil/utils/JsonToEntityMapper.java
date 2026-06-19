package com.mtc.mutuaConseil.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JsonToEntityMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> List<T> mapJsonToEntities(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, clazz)
        );
    }

}
