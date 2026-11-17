package com.duocuc.ms_andesstay_report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Habilita Bean Validation sobre el payload de {@code @KafkaListener}.
 *
 * <p>Sin este registro, la anotación {@code @Valid} del parámetro no se evalúa
 * y un evento incompleto llegaría hasta la capa de negocio (unidad 3, guía 3.3.3).</p>
 */
@Configuration
public class KafkaValidationConfig implements KafkaListenerConfigurer {

    private final LocalValidatorFactoryBean validator;

    public KafkaValidationConfig(LocalValidatorFactoryBean validator) {
        this.validator = validator;
    }

    @Override
    public void configureKafkaListeners(KafkaListenerEndpointRegistrar registrar) {
        registrar.setValidator(validator);
    }
}
