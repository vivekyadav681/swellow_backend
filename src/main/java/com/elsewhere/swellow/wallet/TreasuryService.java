package com.elsewhere.swellow.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class TreasuryService {

    private final WalletRepository walletRepository;

    public TreasuryService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet getTreasuryWallet() {
        return walletRepository.findByWalletType(WalletType.TREASURY)
                .orElseThrow(() -> new IllegalStateException("Treasury wallet is not initialized"));
    }

    @Transactional
    public void transferFromTreasury(Long receiverWalletId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        Wallet treasury = getTreasuryWallet();
        Wallet receiver = walletRepository.findById(receiverWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        if (treasury.getSwlBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient SWL balance in Treasury");
        }

        treasury.setSwlBalance(treasury.getSwlBalance().subtract(amount));
        receiver.setSwlBalance(receiver.getSwlBalance().add(amount));

        walletRepository.save(treasury);
        walletRepository.save(receiver);
    }

    @Transactional
    public void transferToTreasury(Long senderWalletId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        Wallet sender = walletRepository.findById(senderWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));
        Wallet treasury = getTreasuryWallet();

        if (sender.getSwlBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient SWL balance to transfer to Treasury");
        }

        sender.setSwlBalance(sender.getSwlBalance().subtract(amount));
        treasury.setSwlBalance(treasury.getSwlBalance().add(amount));

        walletRepository.save(sender);
        walletRepository.save(treasury);
    }
}
