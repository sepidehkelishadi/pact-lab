package uk.co.pactlab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.pactlab.entity.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);
}
