package com.duocuc.ms_andesstay_mqadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Microservicio administrador de RabbitMQ de AndesStay (puerto 8086).
 *
 * <p>Expone operaciones de administración (colas, exchanges y bindings) sobre el
 * broker. En esta tarea (08) solo se crea el esqueleto: configuración, arranque y
 * Dockerfile. La lógica de administración se añade en la tarea 09
 * ({@code RabbitAdminService}) y los endpoints REST en la tarea 10.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsAndesstayMqadminApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsAndesstayMqadminApplication.class, args);
	}

}
