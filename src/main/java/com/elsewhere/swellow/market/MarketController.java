package com.elsewhere.swellow.market;

import com.elsewhere.swellow.transaction.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @PostMapping("/buy")
    public ResponseEntity<Transaction> buy(@RequestBody Map<String, BigDecimal> payload) {
        BigDecimal amount = payload.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        return ResponseEntity.ok(marketService.buySwl(amount));
    }

    @PostMapping("/sell")
    public ResponseEntity<Transaction> sell(@RequestBody Map<String, BigDecimal> payload) {
        BigDecimal amount = payload.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        return ResponseEntity.ok(marketService.sellSwl(amount));
    }

    @GetMapping("/price")
    public ResponseEntity<MarketState> getPrice() {
        return ResponseEntity.ok(marketService.getMarketState());
    }

    @GetMapping("/history")
    public ResponseEntity<List<PriceHistory>> getHistory() {
        return ResponseEntity.ok(marketService.getPriceHistory());
    }
}
