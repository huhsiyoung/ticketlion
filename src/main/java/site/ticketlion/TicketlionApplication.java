package site.ticketlion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TicketlionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketlionApplication.class, args);
    }

}
