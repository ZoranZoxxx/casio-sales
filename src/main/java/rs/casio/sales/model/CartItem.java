package rs.casio.sales.model;

import java.math.BigDecimal;

public record CartItem(Article article, BigDecimal quantity, BigDecimal sellingPrice) {
    public BigDecimal purchaseTotal() { return article.purchasePrice().multiply(quantity); }
    public BigDecimal sellingTotal() { return sellingPrice.multiply(quantity); }
    public BigDecimal profit() { return sellingTotal().subtract(purchaseTotal()); }
}
