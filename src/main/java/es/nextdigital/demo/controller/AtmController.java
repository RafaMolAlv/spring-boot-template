package es.nextdigital.demo.controller;

import es.nextdigital.demo.controller.dto.PinChangeRequest;
import es.nextdigital.demo.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


/**
 * API a la que se conectan los cajeros. Todas las operaciones se hacen en un cajero ({atmId})
 * con una tarjeta ({cardNumber}); el PIN viaja siempre en el cuerpo.
 */
@RestController
@RequestMapping("/api/atms/{atmId}/cards/{cardNumber}")
public class AtmController {

    private final CardService cardService;

    public AtmController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable Long atmId, @PathVariable String cardNumber,
                         @Valid @RequestBody PinChangeRequest request) {
        cardService.activate(cardNumber, request.currentPin(), request.newPin());
    }

    @PutMapping("/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePin(@PathVariable Long atmId, @PathVariable String cardNumber,
                          @Valid @RequestBody PinChangeRequest request) {
        cardService.changePin(cardNumber, request.currentPin(), request.newPin());
    }
}
