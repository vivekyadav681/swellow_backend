package com.elsewhere.swellow.blockchain;

import com.elsewhere.swellow.transaction.Transaction;
import com.elsewhere.swellow.transaction.TransactionRepository;
import com.elsewhere.swellow.transaction.TransactionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BlockCreationService {

    private final BlockRepository blockRepository;
    private final TransactionRepository transactionRepository;
    private final BlockchainService blockchainService;
    private final int blockThreshold;

    public BlockCreationService(
            BlockRepository blockRepository,
            TransactionRepository transactionRepository,
            BlockchainService blockchainService,
            @Value("${blockchain.block-threshold:10}") int blockThreshold
    ) {
        this.blockRepository = blockRepository;
        this.transactionRepository = transactionRepository;
        this.blockchainService = blockchainService;
        this.blockThreshold = blockThreshold;
    }

    @Transactional
    public synchronized Block createBlockFromPendingTransactions() {
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);
        if (pendingTransactions.isEmpty()) {
            return null;
        }

        Block latestBlock = blockchainService.getLatestBlock()
                .orElseThrow(() -> new IllegalStateException("Genesis block not initialized"));

        Long nextIndex = latestBlock.getBlockIndex() + 1;
        String previousHash = latestBlock.getCurrentHash();
        Long timestamp = Instant.now().toEpochMilli();

        String hash = BlockchainService.calculateHashForBlock(nextIndex, timestamp, pendingTransactions, previousHash);

        Block newBlock = Block.builder()
                .blockIndex(nextIndex)
                .previousHash(previousHash)
                .currentHash(hash)
                .timestamp(timestamp)
                .build();

        Block savedBlock = blockRepository.save(newBlock);

        for (Transaction tx : pendingTransactions) {
            tx.setStatus(TransactionStatus.CONFIRMED);
            tx.setBlock(savedBlock);
            transactionRepository.save(tx);
        }

        // Reload the block to populate lazy loaded lists
        return blockRepository.findById(savedBlock.getId())
                .orElseThrow(() -> new IllegalStateException("Failed to load created block"));
    }

    public void triggerBlockCreationIfThresholdMet() {
        long pendingCount = transactionRepository.countByStatus(TransactionStatus.PENDING);
        if (pendingCount >= blockThreshold) {
            createBlockFromPendingTransactions();
        }
    }

    @Scheduled(fixedRateString = "${blockchain.block-creation-rate-ms:60000}")
    public void scheduleBlockCreation() {
        createBlockFromPendingTransactions();
    }
}
