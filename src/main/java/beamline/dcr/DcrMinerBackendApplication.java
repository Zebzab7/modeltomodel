package beamline.dcr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"beamline.core"})
public class DcrMinerBackendApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(DcrMinerBackendApplication.class, args);
	}
}