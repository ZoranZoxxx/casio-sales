package rs.casio.sales.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Cart {
    private final List<CartItem> items = new ArrayList<>();
    public List<CartItem> items() { return List.copyOf(items); }
    public void start() { items.clear(); }
    public void add(CartItem item) { items.add(item); }
    public void removeLast() { if (!items.isEmpty()) items.remove(items.size() - 1); }
    public boolean isEmpty() { return items.isEmpty(); }
    public BigDecimal total() { return items.stream().map(CartItem::sellingTotal).reduce(BigDecimal.ZERO, BigDecimal::add); }
}
