package uk.co.pactlab.pactlab.api.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.pactlab.pactlab.api.auth.model.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);
}
