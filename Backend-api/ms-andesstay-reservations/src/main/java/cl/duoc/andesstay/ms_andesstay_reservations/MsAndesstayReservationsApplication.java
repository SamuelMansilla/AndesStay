package cl.duoc.andesstay.ms_andesstay_reservations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsAndesstayReservationsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsAndesstayReservationsApplication.class, args);
	}

}
