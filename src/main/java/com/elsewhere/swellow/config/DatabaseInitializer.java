package com.elsewhere.swellow.config;

import com.elsewhere.swellow.blockchain.Block;
import com.elsewhere.swellow.blockchain.BlockRepository;
import com.elsewhere.swellow.blockchain.BlockchainService;
import com.elsewhere.swellow.market.MarketState;
import com.elsewhere.swellow.market.MarketStateRepository;
import com.elsewhere.swellow.market.PriceHistory;
import com.elsewhere.swellow.market.PriceHistoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private final BlockRepository blockRepository;
    private final MarketStateRepository marketStateRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final com.elsewhere.swellow.wallet.WalletRepository walletRepository;

    public DatabaseInitializer(
            BlockRepository blockRepository,
            MarketStateRepository marketStateRepository,
            PriceHistoryRepository priceHistoryRepository,
            com.elsewhere.swellow.wallet.WalletRepository walletRepository
    ) {
        this.blockRepository = blockRepository;
        this.marketStateRepository = marketStateRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        initializeTreasuryWallet();
        initializeGenesisBlock();
        initializeMarketState();
    }

    private void initializeTreasuryWallet() {
        if (walletRepository.findByWalletType(com.elsewhere.swellow.wallet.WalletType.TREASURY).isEmpty()) {
            System.out.println("Initializing Treasury Wallet...");
            com.elsewhere.swellow.wallet.Wallet treasury = com.elsewhere.swellow.wallet.Wallet.builder()
                    .cashBalance(BigDecimal.ZERO)
                    .swlBalance(new BigDecimal("1000000.00000000"))
                    .publicKey("SYSTEM")
                    .privateKey("SYSTEM")
                    .walletType(com.elsewhere.swellow.wallet.WalletType.TREASURY)
                    .build();
            walletRepository.save(treasury);
            System.out.println("Treasury Wallet initialized.");
        }
    }

    private void initializeGenesisBlock() {
        if (blockRepository.count() == 0) {
            System.out.println("Initializing Genesis Block...");
            Long blockIndex = 0L;
            String previousHash = "0";
            Long timestamp = 1700000000000L;

            String hash = BlockchainService.calculateHashForBlock(blockIndex, timestamp, new ArrayList<>(), previousHash);

            Block genesis = Block.builder()
                    .blockIndex(blockIndex)
                    .previousHash(previousHash)
                    .currentHash(hash)
                    .timestamp(timestamp)
                    .build();

            blockRepository.save(genesis);
            System.out.println("Genesis Block initialized with hash: " + hash);
        }
    }

    private void initializeMarketState() {
        if (marketStateRepository.count() == 0) {
            System.out.println("Initializing Market State...");
            MarketState marketState = MarketState.builder()
                    .id(1L)
                    .currentPrice(new BigDecimal("1.0000"))
                    .totalSupply(new BigDecimal("1000000.00000000"))
                    .lastUpdated(Instant.now())
                    .build();
            marketStateRepository.save(marketState);

            PriceHistory initialHistory = PriceHistory.builder()
                    .price(new BigDecimal("1.0000"))
                    .timestamp(Instant.now())
                    .build();
            priceHistoryRepository.save(initialHistory);
            System.out.println("Market State initialized.");
        }
    }
}
