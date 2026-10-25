package es.nextdigital.demo.repository;

import es.nextdigital.demo.domain.Atm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtmRepository extends JpaRepository<Atm, Long> {
}
