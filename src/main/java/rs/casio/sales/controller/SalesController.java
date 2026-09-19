package rs.casio.sales.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;
import rs.casio.sales.model.*;
import rs.casio.sales.service.BusinessException;
import rs.casio.sales.service.SalesService;
import java.time.LocalDate;
import java.util.Map;

@Controller
public class SalesController {
    private final SalesService service;
    public SalesController(SalesService service) { this.service = service; }
    @GetMapping("/") public String page() { return "sales"; }
    @GetMapping("/api/day") @ResponseBody public DayStatus day() { return service.status(LocalDate.now()); }
    @GetMapping("/api/articles") @ResponseBody public Object search(@RequestParam String query) { return service.search(query); }
    @GetMapping("/api/articles/code/{code}") @ResponseBody public Article code(@PathVariable String code) { return service.articleByCode(code); }
    @PostMapping("/api/cart/start") @ResponseBody public CartView start(HttpSession session) { ensureOpen(); Cart cart = cart(session); cart.start(); return view(cart); }
    @PostMapping("/api/cart/items") @ResponseBody public CartView add(@Valid @RequestBody AddItemRequest request, HttpSession session) { ensureOpen(); Cart cart = cart(session); cart.add(service.addToCart(request)); return view(cart); }
    @PostMapping("/api/cart/remove-last") @ResponseBody public CartView removeLast(HttpSession session) { Cart cart = cart(session); cart.removeLast(); return view(cart); }
    @PostMapping("/api/cart/execute") @ResponseBody public DayStatus execute(HttpSession session) { Cart cart = cart(session); DayStatus result = service.execute(cart, LocalDate.now()); cart.start(); return result; }
    @PostMapping("/api/day/close") @ResponseBody public DayStatus close(@Valid @RequestBody CloseDayRequest request) { return service.closeDay(LocalDate.now(), request.deposit()); }
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) @ResponseBody public Map<String, String> business(BusinessException e) { return Map.of("message", e.getMessage()); }
    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody public Map<String, String> response(ResponseStatusException e) { return Map.of("message", e.getReason()); }
    private void ensureOpen() { if (service.status(LocalDate.now()).closed()) throw new BusinessException("Ovaj dan je zakljucen; novi unos nije dozvoljen."); }
    private Cart cart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) { cart = new Cart(); session.setAttribute("cart", cart); }
        return cart;
    }
    public record CartView(java.util.List<CartItem> items, java.math.BigDecimal total) {}
    private CartView view(Cart cart) { return new CartView(cart.items(), cart.total()); }
}
