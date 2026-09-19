package rs.casio.sales.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record AddItemRequest(@NotBlank String articleCode,
                             @DecimalMin(value = "0.001", message = "Kolicina mora biti veca od nule") BigDecimal quantity,
                             @DecimalMin(value = "0.00", inclusive = false, message = "Prodajna cijena mora biti veca od nule") BigDecimal sellingPrice) {}
