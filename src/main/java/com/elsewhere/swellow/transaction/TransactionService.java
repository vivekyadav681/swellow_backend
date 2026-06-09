package com.elsewhere.swellow.transaction;

import com.elsewhere.swellow.common.CryptoUtil;
import com.elsewhere.swellow.wallet.Wallet;
import com.elsewhere.swellow.wallet.WalletRepository;
import com.elsewhere.swellow.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final com.elsewhere.swellow.blockchain.BlockCreationService blockCreationService;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletService walletService,
            WalletRepository walletRepository,
            com.elsewhere.swellow.blockchain.BlockCreationService blockCreationService
    ) {
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.blockCreationService = blockCreationService;
    }

    public List<Transaction> getTransactionsForCurrentUser() {
        Wallet wallet = walletService.getCurrentUserWallet();
        return transactionRepository.findBySenderWalletIdOrReceiverWalletId(wallet.getId(), wallet.getId());
    }

    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findByStatus(TransactionStatus.PENDING);
    }

    @Transactional
    public Transaction transfer(Long receiverWalletId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        Wallet sender = walletService.getCurrentUserWallet();

        if (sender.getId().equals(receiverWalletId)) {
            throw new IllegalArgumentException("Cannot transfer SWL to yourself");
        }

        Wallet receiver = walletRepository.findById(receiverWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        if (sender.getSwlBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient SWL balance");
        }

        // Deduct sender, credit receiver immediately in the database
        walletService.updateBalancesForTransfer(sender.getId(), receiverWalletId, amount);

        // Generate ECDSA signature using sender's private key
        Instant now = Instant.now();
        String formattedAmount = amount.setScale(8, java.math.RoundingMode.HALF_UP).toPlainString();
        String dataToSign = sender.getId() + ":" + receiverWalletId + ":" + formattedAmount + ":" + now.toEpochMilli();
        String signature = CryptoUtil.sign(dataToSign, sender.getPrivateKey());

        // Create transaction record in the pool
        Transaction transaction = Transaction.builder()
                .senderWalletId(sender.getId())
                .receiverWalletId(receiverWalletId)
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
