package es.nextdigital.demo.controller.dto;

import es.nextdigital.demo.domain.Movement;
import es.nextdigital.demo.domain.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovementResponse(Long id, MovementType type, BigDecimal amount, String description,
                               LocalDateTime date) {

    public static MovementResponse from(Movement movement) {
        return new MovementResponse(movement.getId(), movement.getType(), movement.getAmount(),
                movement.getDescription(), movement.getCreatedAt());
    }
}
