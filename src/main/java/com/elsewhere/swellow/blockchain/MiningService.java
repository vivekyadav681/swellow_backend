package com.elsewhere.swellow.blockchain;

import com.elsewhere.swellow.transaction.Transaction;
import com.elsewhere.swellow.transaction.TransactionRepository;
import com.elsewhere.swellow.transaction.TransactionStatus;
import com.elsewhere.swellow.wallet.Wallet;
import com.elsewhere.swellow.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class MiningService {

    private final BlockRepository blockRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final BlockchainService blockchainService;

    public MiningService(
            BlockRepository blockRepository,
            TransactionRepository transactionRepository,
            WalletService walletService,
            BlockchainService blockchainService
    ) {
        this.blockRepository = blockRepository;
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.blockchainService = blockchainService;
    }

    @Transactional
    public Block minePendingTransactions() {
        // 1. Get current miner's wallet
        Wallet minerWallet = walletService.getCurrentUserWallet();

        // 2. Fetch all PENDING transactions
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);

        // 3. Create the Mining Reward Transaction (Coinbase)
        Transaction rewardTx = Transaction.builder()
                .senderWalletId(null)
                .receiverWalletId(minerWallet.getId())
                .amount(new BigDecimal("10.00000000"))
                .signature("SYSTEM_REWARD")
                .status(TransactionStatus.PENDING)
                .timestamp(Instant.now())
                .build();

        rewardTx = transactionRepository.save(rewardTx);

        // 4. Combine all transactions to be mined
        List<Transaction> transactionsToMine = new ArrayList<>();
        transactionsToMine.add(rewardTx);
        transactionsToMine.addAll(pendingTransactions);

        // 5. Get the previous block's details
        Block latestBlock = blockchainService.getLatestBlock()
                .orElseThrow(() -> new IllegalStateException("Genesis block not initialized"));

        Long nextIndex = latestBlock.getBlockIndex() + 1;
        String previousHash = latestBlock.getCurrentHash();
        Long timestamp = Instant.now().toEpochMilli();

        // 6. Perform Proof of Work (find nonce satisfying target difficulty)
        Long nonce = 0L;
        String hash = "";
        String targetPrefix = "0000";

        while (true) {
            hash = BlockchainService.calculateHashForBlock(nextIndex, timestamp, transactionsToMine, previousHash, nonce);
            if (hash.startsWith(targetPrefix)) {
                break;
            }
            nonce++;
        }

        // 7. Create the new Block
        Block newBlock = Block.builder()
                .blockIndex(nextIndex)
                .previousHash(previousHash)
                .currentHash(hash)
                .nonce(nonce)
                .timestamp(timestamp)
                .build();

        Block savedBlock = blockRepository.save(newBlock);

        // 8. Confirm all transactions in the block
        for (Transaction tx : transactionsToMine) {
            tx.setStatus(TransactionStatus.CONFIRMED);
            tx.setBlock(savedBlock);
            transactionRepository.save(tx);
        }

        // Credit miner
        walletService.creditMiningReward(minerWallet.getId(), rewardTx.getAmount());

        // Reload the block to populate lazy loaded lists
        return blockRepository.findById(savedBlock.getId())
                .orElseThrow(() -> new IllegalStateException("Failed to load mined block"));
    }
}
