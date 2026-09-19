package rs.casio.sales.model;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record CloseDayRequest(@DecimalMin(value = "0.00", inclusive = true, message = "Uplata ne moze biti negativna") BigDecimal deposit) {}
