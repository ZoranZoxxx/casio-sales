package rs.casio.sales.model;

import java.math.BigDecimal;

public record Article(String code, String name, BigDecimal quantity, BigDecimal purchasePrice, BigDecimal stockValue) {}
