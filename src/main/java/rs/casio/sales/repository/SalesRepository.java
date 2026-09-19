package rs.casio.sales.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import rs.casio.sales.model.Article;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SalesRepository {
    private final JdbcTemplate jdbc;
    public SalesRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; ensureSupportingTable(); }
    private void ensureSupportingTable() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS UPIS_NA_DAN (NAZIV TEXT PRIMARY KEY, VREDNOST TEXT)");
    }
    private Article mapArticle(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new Article(rs.getString("SIFRA_ARTIKLA"), rs.getString("NAZIV_ARTIKLA"), decimal(rs.getString("KOLICINA")), decimal(rs.getString("NABAVNA_CIJENA_U_EURIMA")), decimal(rs.getString("SUMA")));
    }
    private BigDecimal decimal(String value) { return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value); }
    public Optional<Article> findByCode(String code) {
        return jdbc.query("SELECT SIFRA_ARTIKLA, NAZIV_ARTIKLA, KOLICINA, NABAVNA_CIJENA_U_EURIMA, 0 AS SUMA FROM BAZA_ARTIKALA WHERE SIFRA_ARTIKLA = ?", this::mapArticle, code).stream().findFirst();
    }
    public List<Article> searchByName(String query) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return jdbc.query("SELECT SIFRA_ARTIKLA, NAZIV_ARTIKLA, KOLICINA, NABAVNA_CIJENA_U_EURIMA, 0 AS SUMA FROM BAZA_ARTIKALA WHERE NAZIV_ARTIKLA LIKE ? ESCAPE '\\' ORDER BY NAZIV_ARTIKLA LIMIT 30", this::mapArticle, "%" + escaped + "%");
    }
    public void createDailySalesTable(LocalDate date) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + dailyTable(date) + " (SIFRA_ARTIKLA, NAZIV_ARTIKLA, KOLICINA, NABAVNA_CIJENA_U_EURIMA, PRODAJNA_CIJENA_U_EURIMA, UKUPNO_NABAVNA_CENA, UKUPNO_PRODAJNA_CENA, UKUPNA_RAZLIKA, VRIJEME_PRODAJE)");
    }
    public void updateStock(Article article, BigDecimal newQuantity) {
        jdbc.update("UPDATE BAZA_ARTIKALA SET KOLICINA = ? WHERE SIFRA_ARTIKLA = ?", newQuantity.toPlainString(), article.code());
    }
    public void insertSale(LocalDate date, Article article, BigDecimal quantity, BigDecimal sellingPrice) {
        BigDecimal purchaseTotal = quantity.multiply(article.purchasePrice());
        BigDecimal sellingTotal = quantity.multiply(sellingPrice);
        jdbc.update("INSERT INTO " + dailyTable(date) + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", article.code(), article.name(), quantity.toPlainString(), article.purchasePrice().toPlainString(), sellingPrice.toPlainString(), purchaseTotal.toPlainString(), sellingTotal.toPlainString(), sellingTotal.subtract(purchaseTotal).toPlainString(), LocalTime.now().withNano(0).toString());
    }
    public BigDecimal value(String key) {
        List<String> values = jdbc.query("SELECT VREDNOST FROM UPIS_NA_DAN WHERE NAZIV = ?", (rs, row) -> rs.getString(1), key);
        return values.isEmpty() ? BigDecimal.ZERO : decimal(values.get(0));
    }
    public boolean hasValue(String key) { return jdbc.queryForObject("SELECT COUNT(*) FROM UPIS_NA_DAN WHERE NAZIV = ?", Integer.class, key) > 0; }
    public void putValue(String key, BigDecimal value) {
        int updated = jdbc.update("UPDATE UPIS_NA_DAN SET VREDNOST = ? WHERE NAZIV = ?", value.toPlainString(), key);
        if (updated == 0) jdbc.update("INSERT INTO UPIS_NA_DAN (NAZIV, VREDNOST) VALUES (?, ?)", key, value.toPlainString());
    }
    public static String dayKey(LocalDate date) { return "Ukupno_" + date.getDayOfMonth() + "_" + date.getMonthValue() + "_" + date.getYear(); }
    public static String closeKey(LocalDate date) { return "ZakljuciDan_" + date.getDayOfMonth() + "_" + date.getMonthValue() + "_" + date.getYear(); }
    public static String depositKey(LocalDate date) { return "UplataDnevnogPazara_" + date.getDayOfMonth() + "_" + date.getMonthValue() + "_" + date.getYear(); }
    private static String dailyTable(LocalDate date) { return "PROMET__" + date.getDayOfMonth() + "_" + date.getMonthValue() + "_" + date.getYear(); }
}
