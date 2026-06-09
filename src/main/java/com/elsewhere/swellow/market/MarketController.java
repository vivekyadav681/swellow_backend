package com.elsewhere.swellow.market;

import com.elsewhere.swellow.transaction.Transaction;
import com.elsewhere.swellow.wallet.TreasuryService;
import com.elsewhere.swellow.wallet.Wallet;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/market")
public class MarketController {

    private final MarketService marketService;
    private final TreasuryService treasuryService;

    public MarketController(MarketService marketService, TreasuryService treasuryService) {
        this.marketService = marketService;
        this.treasuryService = treasuryService;
    }

    @GetMapping("/treasury")
    public ResponseEntity<Map<String, Object>> getTreasury() {
        Wallet treasury = treasuryService.getTreasuryWallet();
        return ResponseEntity.ok(Map.of(
                "swlBalance", treasury.getSwlBalance(),
                "cashBalance", treasury.getCashBalance()
        ));
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
