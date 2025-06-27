package com.app.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.app.backend.entity.Role;
import com.app.backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import java.util.List;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	// Auto-create roles on startup
	@Bean
	CommandLineRunner initRoles(RoleRepository roleRepository) {
		return args -> {
			List<String> roles = List.of("ADMIN", "TUTOR", "STAFF", "STUDENT");

			for (String roleName : roles) {
				roleRepository.findByName(roleName).ifPresentOrElse(
						r -> {}, // already exists
						() -> roleRepository.save(new Role(null, roleName)) // insert
				);
			}

			System.out.println("✅ Default roles initialized.");
		};
	}
}

//@SpringBootApplication
//public class BackendApplication {
//
//	public static void main(String[] args) {
//		SpringApplication.run(BackendApplication.class, args);
//	}
//
//}
