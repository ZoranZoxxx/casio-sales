package rs.casio.sales.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import rs.casio.sales.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:sqlite:target/casio-service-test.db", "app.copy-reference-database=false"})
class SalesServiceIntegrationTest {
    @Autowired SalesService sales;
    @Autowired JdbcTemplate jdbc;
    private final LocalDate today = LocalDate.now();
    @BeforeEach void database() {
        jdbc.execute("DROP TABLE IF EXISTS PROMET__" + today.getDayOfMonth() + "_" + today.getMonthValue() + "_" + today.getYear());
        jdbc.execute("DROP TABLE IF EXISTS BAZA_ARTIKALA");
        jdbc.execute("DROP TABLE IF EXISTS UPIS_NA_DAN");
        jdbc.execute("CREATE TABLE BAZA_ARTIKALA (SIFRA_ARTIKLA TEXT PRIMARY KEY, NAZIV_ARTIKLA TEXT, KOLICINA TEXT, NABAVNA_CIJENA_U_EURIMA TEXT)");
        jdbc.execute("CREATE TABLE UPIS_NA_DAN (NAZIV TEXT PRIMARY KEY, VREDNOST TEXT)");
        jdbc.update("INSERT INTO BAZA_ARTIKALA VALUES ('A1', 'Kafa domaca', '10', '2.50')");
        jdbc.update("INSERT INTO BAZA_ARTIKALA VALUES ('A2', 'Caj zeleni', '5', '1.00')");
    }
    @Test void searchAndSelectionReturnCorrectPurchasePrice() {
        assertEquals(1, sales.search("Kafa").size());
        Article article = sales.articleByCode("A1");
        assertEquals("Kafa domaca", article.name());
        assertEquals(new BigDecimal("2.50"), article.purchasePrice());
    }
    @Test void manualPriceCalculatesAndAtomicallyReducesStockAndUpdatesTotal() {
        Cart cart = new Cart();
        CartItem item = sales.addToCart(new AddItemRequest("A1", new BigDecimal("2"), new BigDecimal("4.75")));
        assertEquals(new BigDecimal("5.00"), item.purchaseTotal());
        assertEquals(new BigDecimal("9.50"), item.sellingTotal());
        assertEquals(new BigDecimal("4.50"), item.profit());
        cart.add(item);
        DayStatus status = sales.execute(cart, today);
        assertEquals(new BigDecimal("9.50"), status.dailyTotal());
        assertEquals("8", jdbc.queryForObject("SELECT KOLICINA FROM BAZA_ARTIKALA WHERE SIFRA_ARTIKLA='A1'", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM PROMET__" + today.getDayOfMonth() + "_" + today.getMonthValue() + "_" + today.getYear(), Integer.class));
    }
    @Test void closingStoresDepositAndPreventsFurtherSales() {
        Cart cart = new Cart(); cart.add(sales.addToCart(new AddItemRequest("A2", BigDecimal.ONE, new BigDecimal("3.00"))));
        sales.execute(cart, today);
        DayStatus closed = sales.closeDay(today, new BigDecimal("2.00"));
        assertTrue(closed.closed());
        assertEquals(new BigDecimal("1.00"), closed.cashInRegister());
        assertThrows(BusinessException.class, () -> sales.execute(cart, today));
    }
}
