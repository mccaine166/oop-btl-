package com.cinema.analytics.config;

import com.cinema.analytics.model.InventoryTransaction;
import com.cinema.analytics.model.InventoryTransactionType;
import com.cinema.analytics.model.Product;
import com.cinema.analytics.model.ProductType;
import com.cinema.analytics.model.Ticket;
import com.cinema.analytics.model.Transaction;
import com.cinema.analytics.repository.InventoryRepository;
import com.cinema.analytics.repository.ProductRepository;
import com.cinema.analytics.repository.TransactionRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Configuration
@Profile("memory")
public class DataSeeder {
    private static final long SEED = 20240914L;
    private static final int FIXED_YEAR = 2026;
    private static final LocalDate BASE_WEEK_START = LocalDate.of(FIXED_YEAR, 9, 14);

    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final InventoryRepository inventoryRepository;

    public DataSeeder(ProductRepository productRepository,
                      TransactionRepository transactionRepository,
                      InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Bean
    public ApplicationRunner seedDatabase() {
        return args -> seedData();
    }

    public void seedData() {
        if (!productRepository.findAll().isEmpty()) {
            return;
        }

        List<Product> products = createProducts();
        productRepository.saveAll(products);

        List<Transaction> transactions = createTransactions(products);
        transactionRepository.saveAll(transactions);

        List<InventoryTransaction> inventoryTransactions = createInventoryTransactions(products, transactions);
        inventoryRepository.saveAll(inventoryTransactions);
    }

    private List<Product> createProducts() {
        return List.of(
                new Product("prod-combo-a", "Combo A", ProductType.COMBO, new BigDecimal("129000"), "combo"),
                new Product("prod-combo-b", "Combo B", ProductType.COMBO, new BigDecimal("99000"), "combo"),
                new Product("prod-popcorn", "Popcorn", ProductType.SINGLE, new BigDecimal("45000"), "pack"),
                new Product("prod-soft-drink", "Soft Drink", ProductType.SINGLE, new BigDecimal("28000"), "bottle"),
                new Product("prod-hotdog", "Hotdog", ProductType.SINGLE, new BigDecimal("59000"), "item")
        );
    }

    private List<Transaction> createTransactions(List<Product> products) {
        Map<String, Product> productMap = new LinkedHashMap<>();
        products.forEach(product -> productMap.put(product.getId(), product));

        Map<String, Double> moviePopularity = new LinkedHashMap<>();
        moviePopularity.put("movie-1", 1.0);
        moviePopularity.put("movie-2", 1.5);
        moviePopularity.put("movie-3", 1.1);
        moviePopularity.put("movie-4", 1.3);
        moviePopularity.put("movie-5", 0.35);
        moviePopularity.put("movie-6", 0.4);

        Random random = new Random(SEED);
        List<Transaction> transactions = new ArrayList<>();

        for (int week = 0; week < 4; week++) {
            LocalDate weekStart = BASE_WEEK_START.plusWeeks(week);

            for (int day = 0; day < 7; day++) {
                LocalDate date = weekStart.plusDays(day);
                int baseCount = isWeekend(date) ? 18 : 10;

                for (int index = 0; index < baseCount; index++) {
                    String movieId = selectMovieId(random, moviePopularity);
                    String showtimeId = "showtime-" + movieId + "-" + date + "-" + index;
                    LocalDateTime createdAt = createDateTime(date, movieId, random);
                    int ticketCount = ticketCountFor(date, movieId, random, moviePopularity);

                    List<Ticket> tickets = new ArrayList<>();
                    for (int seatIndex = 0; seatIndex < ticketCount; seatIndex++) {
                        String seat = (char) ('A' + (seatIndex % 8)) + String.valueOf((seatIndex / 8) + 1);
                        BigDecimal price = randomPriceForMovie(random, movieId);
                        tickets.add(new Ticket("ticket-" + createdAt.toLocalDate() + "-" + seatIndex, seat, price));
                    }

                    List<Transaction.FnbItem> foodItems = createFoodItems(random, productMap);
                    BigDecimal ticketRevenue = tickets.stream()
                            .map(Ticket::getPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal foodRevenue = foodItems.stream()
                            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Transaction transaction = new Transaction(
                            "txn-" + date + "-" + index + "-" + random.nextInt(1000),
                            createdAt,
                            showtimeId,
                            movieId,
                            tickets,
                            foodItems,
                            ticketRevenue.add(foodRevenue)
                    );
                    transactions.add(transaction);
                }
            }
        }

        return transactions;
    }

    private List<Transaction.FnbItem> createFoodItems(Random random, Map<String, Product> productMap) {
        List<Transaction.FnbItem> result = new ArrayList<>();
        int itemCount = 1 + random.nextInt(3);
        for (int i = 0; i < itemCount; i++) {
            String productId = selectFoodProduct(random);
            Product product = productMap.get(productId);
            int quantity = 1 + random.nextInt(product.getType() == ProductType.COMBO ? 3 : 2);
            result.add(new Transaction.FnbItem(productId, quantity, product.getPrice()));
        }
        return result;
    }

    private String selectFoodProduct(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.45) {
            return "prod-combo-a";
        }
        if (roll < 0.70) {
            return "prod-combo-b";
        }
        if (roll < 0.85) {
            return "prod-popcorn";
        }
        if (roll < 0.95) {
            return "prod-soft-drink";
        }
        return "prod-hotdog";
    }

    private String selectMovieId(Random random, Map<String, Double> moviePopularity) {
        List<String> movieIds = List.of("movie-1", "movie-2", "movie-3", "movie-4", "movie-5", "movie-6");
        double totalWeight = movieIds.stream().mapToDouble(moviePopularity::get).sum();
        double target = random.nextDouble() * totalWeight;
        double accumulated = 0.0;
        for (String movieId : movieIds) {
            accumulated += moviePopularity.getOrDefault(movieId, 1.0);
            if (target <= accumulated) {
                return movieId;
            }
        }
        return "movie-1";
    }

    private int ticketCountFor(LocalDate date, String movieId, Random random, Map<String, Double> moviePopularity) {
        int base = isWeekend(date) ? 6 : 4;
        int extra = random.nextInt(4);
        if (isEveningPeak(date)) {
            extra += 2;
        }
        double popularity = moviePopularity.getOrDefault(movieId, 1.0);
        int count = (int) Math.round((base + extra) * popularity);
        return Math.max(1, count);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isEveningPeak(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.FRIDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private LocalDateTime createDateTime(LocalDate date, String movieId, Random random) {
        double popularity = Map.of(
                "movie-1", 1.0,
                "movie-2", 1.5,
                "movie-3", 1.1,
                "movie-4", 1.3,
                "movie-5", 0.35,
                "movie-6", 0.4
        ).getOrDefault(movieId, 1.0);

        int[] eveningTimes = {18, 19, 20, 21, 22};
        int[] afternoonTimes = {14, 15, 16, 17, 18};
        int[] daytimeTimes = {10, 11, 12, 13};

        int hour;
        if (popularity >= 1.3) {
            hour = eveningTimes[random.nextInt(eveningTimes.length)];
        } else if (popularity <= 0.4) {
            hour = random.nextDouble() < 0.7 ? daytimeTimes[random.nextInt(daytimeTimes.length)] : afternoonTimes[random.nextInt(afternoonTimes.length)];
        } else {
            hour = random.nextDouble() < 0.65 ? eveningTimes[random.nextInt(eveningTimes.length)] : afternoonTimes[random.nextInt(afternoonTimes.length)];
        }

        int minute = random.nextInt(60);
        return date.atTime(hour, minute);
    }

    private BigDecimal randomPriceForMovie(Random random, String movieId) {
        int base = switch (movieId) {
            case "movie-1" -> 65000;
            case "movie-2" -> 76000;
            case "movie-3" -> 84000;
            case "movie-4" -> 78000;
            default -> 70000;
        };
        int delta = random.nextInt(20000);
        return BigDecimal.valueOf(base + delta).setScale(0);
    }

    private List<InventoryTransaction> createInventoryTransactions(List<Product> products, List<Transaction> transactions) {
        List<InventoryTransaction> inventoryTransactions = new ArrayList<>();
        LocalDate openingDate = BASE_WEEK_START.minusDays(1);

        Map<String, Map<LocalDate, Integer>> salesByProductAndDate = new LinkedHashMap<>();
        for (Product product : products) {
            salesByProductAndDate.put(product.getId(), new LinkedHashMap<>());
        }

        for (Transaction transaction : transactions) {
            for (Transaction.FnbItem item : transaction.getFoodItems()) {
                Map<LocalDate, Integer> soldPerDate = salesByProductAndDate.get(item.getProductId());
                if (soldPerDate == null) {
                    continue;
                }
                soldPerDate.merge(transaction.getCreatedAt().toLocalDate(), item.getQuantity(), Integer::sum);
            }
        }

        Map<String, Integer> weekSales0 = new LinkedHashMap<>();
        for (Product product : products) {
            int weeklySales = 0;
            for (int day = 0; day < 7; day++) {
                LocalDate date = BASE_WEEK_START.plusDays(day);
                weeklySales += salesByProductAndDate.get(product.getId()).getOrDefault(date, 0);
            }
            weekSales0.put(product.getId(), weeklySales);
        }

        Map<String, Integer> targets = new LinkedHashMap<>();
        for (Product product : products) {
            int sales = weekSales0.getOrDefault(product.getId(), 0);
            int forecastNeed = Math.max(1, (int) Math.round(sales * 1.15));
            int target;
            switch (product.getId()) {
                case "prod-combo-a":
                    target = (int) Math.round(0.6 * forecastNeed);
                    break;
                case "prod-combo-b":
                    target = (int) Math.round(2.0 * forecastNeed);
                    break;
                case "prod-popcorn":
                    target = forecastNeed + 5;
                    break;
                case "prod-soft-drink":
                case "prod-hotdog":
                    target = (int) Math.round(2.0 * forecastNeed);
                    break;
                default:
                    target = forecastNeed;
                    break;
            }
            targets.put(product.getId(), target);
        }

        Map<String, Integer> previousWeekSalesByProduct = new LinkedHashMap<>();
        for (Product product : products) {
            previousWeekSalesByProduct.put(product.getId(), weekSales0.getOrDefault(product.getId(), 0));
        }

        for (Product product : products) {
            int week0Import = previousWeekSalesByProduct.getOrDefault(product.getId(), 0);
            int openingImport = targets.get(product.getId()) + weekSales0.getOrDefault(product.getId(), 0) - week0Import;
            inventoryTransactions.add(new InventoryTransaction(
                    "inv-opening-" + product.getId(),
                    product.getId(),
                    InventoryTransactionType.IMPORT,
                    openingImport,
                    openingDate
            ));
        }

        for (int week = 0; week < 4; week++) {
            LocalDate weekStart = BASE_WEEK_START.plusWeeks(week);
            for (Product product : products) {
                int importQuantity = week == 0
                        ? previousWeekSalesByProduct.getOrDefault(product.getId(), 0)
                        : previousWeekSalesByProduct.getOrDefault(product.getId(), 0);
                inventoryTransactions.add(new InventoryTransaction(
                        "inv-import-" + weekStart + "-" + product.getId(),
                        product.getId(),
                        InventoryTransactionType.IMPORT,
                        importQuantity,
                        weekStart
                ));
                previousWeekSalesByProduct.put(product.getId(), getWeeklySales(product.getId(), weekStart, salesByProductAndDate));
            }
        }

        for (Product product : products) {
            Map<LocalDate, Integer> soldPerDate = salesByProductAndDate.get(product.getId());
            for (int week = 0; week < 4; week++) {
                LocalDate weekStart = BASE_WEEK_START.plusWeeks(week);
                for (int day = 0; day < 7; day++) {
                    LocalDate date = weekStart.plusDays(day);
                    int soldOnDate = soldPerDate.getOrDefault(date, 0);
                    if (soldOnDate > 0) {
                        inventoryTransactions.add(new InventoryTransaction(
                                "inv-export-" + date + "-" + product.getId(),
                                product.getId(),
                                InventoryTransactionType.EXPORT,
                                soldOnDate,
                                date
                        ));
                    }
                }
            }
        }

        return inventoryTransactions;
    }

    private int getWeeklySales(String productId, LocalDate weekStart, Map<String, Map<LocalDate, Integer>> salesByProductAndDate) {
        int weeklySales = 0;
        for (int day = 0; day < 7; day++) {
            LocalDate date = weekStart.plusDays(day);
            weeklySales += salesByProductAndDate.get(productId).getOrDefault(date, 0);
        }
        return weeklySales;
    }
}
