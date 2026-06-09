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
    private final com.elsewhere.swellow.blockchain.BlockCreationService blockCreationService;
    private final BigDecimal priceFactor;
    private final com.elsewhere.swellow.wallet.TreasuryService treasuryService;

    public MarketService(
            MarketStateRepository marketStateRepository,
            PriceHistoryRepository priceHistoryRepository,
            WalletService walletService,
            TransactionRepository transactionRepository,
            com.elsewhere.swellow.blockchain.BlockCreationService blockCreationService,
            @org.springframework.beans.factory.annotation.Value("${market.price-factor:0.001}") BigDecimal priceFactor,
            com.elsewhere.swellow.wallet.TreasuryService treasuryService
    ) {
        this.marketStateRepository = marketStateRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.walletService = walletService;
        this.transactionRepository = transactionRepository;
        this.blockCreationService = blockCreationService;
        this.priceFactor = priceFactor;
        this.treasuryService = treasuryService;
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
        Wallet treasuryWallet = treasuryService.getTreasuryWallet();

        BigDecimal price = marketState.getCurrentPrice();
        BigDecimal cost = amount.multiply(price);

        // Update balances: deduct user cash, transfer SWL from Treasury to user
        walletService.deductCash(wallet.getId(), cost);
        treasuryService.transferFromTreasury(wallet.getId(), amount);

        // Update market state (supply unchanged)
        BigDecimal priceIncrease = amount.multiply(priceFactor);
        BigDecimal newPrice = price.add(priceIncrease);

        marketState.setCurrentPrice(newPrice);
        marketState.setLastUpdated(Instant.now());
        marketStateRepository.save(marketState);

        // Log price change
        PriceHistory history = PriceHistory.builder()
                .price(newPrice)
                .timestamp(Instant.now())
                .build();
        priceHistoryRepository.save(history);

        // Create transaction: sender = Treasury, receiver = user's wallet
        Instant now = Instant.now();
        Transaction transaction = Transaction.builder()
                .senderWalletId(treasuryWallet.getId())
                .receiverWalletId(wallet.getId())
                .amount(amount)
                .signature("TREASURY_TRANSFER")
                .status(TransactionStatus.PENDING)
                .timestamp(now)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        blockCreationService.triggerBlockCreationIfThresholdMet();
        return savedTransaction;
    }

    @Transactional
    public Transaction sellSwl(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        MarketState marketState = getMarketState();
        Wallet wallet = walletService.getCurrentUserWallet();
        Wallet treasuryWallet = treasuryService.getTreasuryWallet();

        if (wallet.getSwlBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient SWL balance to sell");
        }

        BigDecimal price = marketState.getCurrentPrice();
        BigDecimal revenue = amount.multiply(price);

        // Update balances: transfer SWL from user to Treasury, credit user cash
        treasuryService.transferToTreasury(wallet.getId(), amount);
        walletService.creditCash(wallet.getId(), revenue);

        // Update market state (supply unchanged)
        BigDecimal priceDecrease = amount.multiply(priceFactor);
        BigDecimal newPrice = price.subtract(priceDecrease);
        if (newPrice.compareTo(new BigDecimal("0.01")) < 0) {
            newPrice = new BigDecimal("0.01");
        }

        marketState.setCurrentPrice(newPrice);
        marketState.setLastUpdated(Instant.now());
        marketStateRepository.save(marketState);

        // Log price change
        PriceHistory history = PriceHistory.builder()
                .price(newPrice)
                .timestamp(Instant.now())
                .build();
        priceHistoryRepository.save(history);

        // Create signed transaction: sender = user's wallet, receiver = Treasury's wallet
        Instant now = Instant.now();
        String formattedAmount = amount.setScale(8, java.math.RoundingMode.HALF_UP).toPlainString();
        String dataToSign = wallet.getId() + ":" + treasuryWallet.getId() + ":" + formattedAmount + ":" + now.toEpochMilli();
        String signature = CryptoUtil.sign(dataToSign, wallet.getPrivateKey());

        Transaction transaction = Transaction.builder()
                .senderWalletId(wallet.getId())
                .receiverWalletId(treasuryWallet.getId())
                .amount(amount)
                .signature(signature)
                .status(TransactionStatus.PENDING)
                .timestamp(now)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        blockCreationService.triggerBlockCreationIfThresholdMet();
        return savedTransaction;
    }
}
