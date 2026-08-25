package com.ceramic.config;

import com.ceramic.dto.BatchResponse;
import com.ceramic.dto.OrderResponse;
import com.ceramic.entity.Batch;
import com.ceramic.entity.Order;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true);

        // Skip automatic mapping of fields where collection types differ (Set vs List)
        mapper.typeMap(Batch.class, BatchResponse.class).addMappings(m -> {
            m.skip(BatchResponse::setStageHistories);
        });

        mapper.typeMap(Order.class, OrderResponse.class).addMappings(m -> {
            m.skip(OrderResponse::setBatches);
            m.skip(OrderResponse::setAiExtraction);
        });

        return mapper;
    }
}
