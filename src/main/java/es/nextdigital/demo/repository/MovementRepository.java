package es.nextdigital.demo.repository;

import es.nextdigital.demo.domain.Movement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    List<Movement> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);
}
