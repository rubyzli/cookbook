package family.cookbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// @EnableAsync: recipes are translated automatically in the background (see AutomaticTranslation)
@SpringBootApplication
@EnableAsync
public class CookbookApplication {

	public static void main(String[] args) {
		SpringApplication.run(CookbookApplication.class, args);
	}

}
