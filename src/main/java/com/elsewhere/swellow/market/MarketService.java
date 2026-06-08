package com.elsewhere.swellow.market;

import com.elsewhere.swellow.common.CryptoUtil;
import com.elsewhere.swellow.transaction.Transaction;
import com.elsewhere.swellow.transaction.TransactionRepository;
import com.elsewhere.swellow.transaction.TransactionStatus;
import com.elsewhere.swellow.wallet.Wallet;
import com.elsewhere.swellow.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class MarketService {

    private final MarketStateRepository marketStateRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;

    public MarketService(
            MarketStateRepository marketStateRepository,
            PriceHistoryRepository priceHistoryRepository,
            WalletService walletService,
            TransactionRepository transactionRepository
    ) {
        this.marketStateRepository = marketStateRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.walletService = walletService;
        this.transactionRepository = transactionRepository;
    }

    public MarketState getMarketState() {
        return marketStateRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Market state not initialized"));
    }

    public BigDecimal getCurrentPrice() {
        return getMarketState().getCurrentPrice();
    }

    public List<PriceHistory> getPriceHistory() {
        return priceHistoryRepository.findAllByOrderByTimestampAsc();
    }

    @Transactional
    public Transaction buySwl(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        MarketState marketState = getMarketState();
        Wallet wallet = walletService.getCurrentUserWallet();

        BigDecimal price = marketState.getCurrentPrice();
        BigDecimal cost = amount.multiply(price);

        // Update balances: deduct cash, credit SWL
        walletService.updateBalancesForBuy(wallet.getId(), cost, amount);

        // Update market state
        BigDecimal newSupply = marketState.getTotalSupply().add(amount);
        BigDecimal priceIncrease = amount.multiply(new BigDecimal("0.001"));
        BigDecimal newPrice = price.add(priceIncrease);

        marketState.setTotalSupply(newSupply);
        marketState.setCurrentPrice(newPrice);
        marketState.setLastUpdated(Instant.now());
        marketStateRepository.save(marketState);

        // Log price change
        PriceHistory history = PriceHistory.builder()
                .price(newPrice)
                .timestamp(Instant.now())
                .build();
        priceHistoryRepository.save(history);

        // Create transaction: sender = null (system), receiver = user's wallet
        Instant now = Instant.now();
        Transaction transaction = Transaction.builder()
                .senderWalletId(null)
                .receiverWalletId(wallet.getId())
                .amount(amount)
                .signature("SYSTEM_BUY")
                .status(TransactionStatus.PENDING)
                .timestamp(now)
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction sellSwl(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        MarketState marketState = getMarketState();
        Wallet wallet = walletService.getCurrentUserWallet();

        if (wallet.getSwlBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient SWL balance to sell");
        }

        BigDecimal price = marketState.getCurrentPrice();
        BigDecimal revenue = amount.multiply(price);

        // Update balances: deduct SWL, credit cash
        walletService.updateBalancesForSell(wallet.getId(), revenue, amount);

        // Update market state
        BigDecimal newSupply = marketState.getTotalSupply().subtract(amount);
        BigDecimal priceDecrease = amount.multiply(new BigDecimal("0.001"));
        BigDecimal newPrice = price.subtract(priceDecrease);
        if (newPrice.compareTo(new BigDecimal("0.01")) < 0) {
            newPrice = new BigDecimal("0.01");
        }

        marketState.setTotalSupply(newSupply);
        marketState.setCurrentPrice(newPrice);
        marketState.setLastUpdated(Instant.now());
        marketStateRepository.save(marketState);

        // Log price change
        PriceHistory history = PriceHistory.builder()
                .price(newPrice)
                .timestamp(Instant.now())
                .build();
        priceHistoryRepository.save(history);

        // Create signed transaction: sender = user's wallet, receiver = null (system)
        Instant now = Instant.now();
        String formattedAmount = amount.setScale(8, java.math.RoundingMode.HALF_UP).toPlainString();
        String dataToSign = wallet.getId() + ":null:" + formattedAmount + ":" + now.toEpochMilli();
        String signature = CryptoUtil.sign(dataToSign, wallet.getPrivateKey());

        Transaction transaction = Transaction.builder()
                .senderWalletId(wallet.getId())
                .receiverWalletId(null)
                .amount(amount)
                .signature(signature)
                .status(TransactionStatus.PENDING)
                .timestamp(now)
                .build();

        return transactionRepository.save(transaction);
    }
}
