package rs.casio.sales.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DayStatus(LocalDate date, boolean closed, BigDecimal dailyTotal, BigDecimal deposit, BigDecimal cashInRegister) {}
