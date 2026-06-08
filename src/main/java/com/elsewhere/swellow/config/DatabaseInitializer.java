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

    public DatabaseInitializer(
            BlockRepository blockRepository,
            MarketStateRepository marketStateRepository,
            PriceHistoryRepository priceHistoryRepository
    ) {
        this.blockRepository = blockRepository;
        this.marketStateRepository = marketStateRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        initializeGenesisBlock();
        initializeMarketState();
    }

    private void initializeGenesisBlock() {
        if (blockRepository.count() == 0) {
            System.out.println("Initializing Genesis Block...");
            Long blockIndex = 0L;
            String previousHash = "0";
            Long timestamp = 1700000000000L;

            // Mine genesis block nonce to satisfy "0000" difficulty
            Long nonce = 0L;
            String hash = "";
            while (true) {
                hash = BlockchainService.calculateHashForBlock(blockIndex, timestamp, new ArrayList<>(), previousHash, nonce);
                if (hash.startsWith("0000")) {
                    break;
                }
                nonce++;
            }

            Block genesis = Block.builder()
                    .blockIndex(blockIndex)
                    .previousHash(previousHash)
                    .currentHash(hash)
                    .nonce(nonce)
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
                    .totalSupply(new BigDecimal("1000.00000000"))
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
