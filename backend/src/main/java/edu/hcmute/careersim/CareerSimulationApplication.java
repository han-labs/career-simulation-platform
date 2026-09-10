package edu.hcmute.careersim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CareerSimulationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerSimulationApplication.class, args);
    }
}
