package rs.casio.sales.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.casio.sales.model.*;
import rs.casio.sales.repository.SalesRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SalesService {
    private final SalesRepository repository;
    public SalesService(SalesRepository repository) { this.repository = repository; }
    public Article articleByCode(String code) { return repository.findByCode(code.trim()).orElseThrow(() -> new BusinessException("Artikal nije pronadjen.")); }
    public List<Article> search(String text) { return text == null || text.trim().length() < 3 ? List.of() : repository.searchByName(text.trim()); }
    public CartItem addToCart(AddItemRequest request) {
        Article article = articleByCode(request.articleCode());
        if (article.quantity().compareTo(request.quantity()) < 0) throw new BusinessException("Nema dovoljno artikala u magacinu. Stanje: " + article.quantity());
        return new CartItem(article, request.quantity(), request.sellingPrice());
    }
    @Transactional
    public DayStatus execute(Cart cart, LocalDate date) {
        if (cart.isEmpty()) throw new BusinessException("Racun nema stavki.");
        assertOpen(date);
        repository.createDailySalesTable(date);
        for (CartItem item : cart.items()) {
            Article fresh = articleByCode(item.article().code());
            if (fresh.quantity().compareTo(item.quantity()) < 0) throw new BusinessException("Nedovoljno stanje za: " + fresh.name());
            repository.updateStock(fresh, fresh.quantity().subtract(item.quantity()));
            repository.insertSale(date, fresh, item.quantity(), item.sellingPrice());
        }
        repository.putValue(SalesRepository.dayKey(date), repository.value(SalesRepository.dayKey(date)).add(cart.total()));
        return status(date);
    }
    @Transactional
    public DayStatus closeDay(LocalDate date, BigDecimal deposit) {
        assertOpen(date);
        BigDecimal total = repository.value(SalesRepository.dayKey(date));
        if (deposit.compareTo(total) > 0) throw new BusinessException("Uplata ne moze biti veca od dnevnog prometa.");
        repository.putValue(SalesRepository.closeKey(date), BigDecimal.ONE);
        repository.putValue(SalesRepository.depositKey(date), deposit);
        return status(date);
    }
    public DayStatus status(LocalDate date) {
        BigDecimal total = repository.value(SalesRepository.dayKey(date));
        BigDecimal deposit = repository.value(SalesRepository.depositKey(date));
        return new DayStatus(date, repository.hasValue(SalesRepository.closeKey(date)), total, deposit, total.subtract(deposit));
    }
    private void assertOpen(LocalDate date) { if (repository.hasValue(SalesRepository.closeKey(date))) throw new BusinessException("Ovaj dan je zakljucen; novi unos nije dozvoljen."); }
}
