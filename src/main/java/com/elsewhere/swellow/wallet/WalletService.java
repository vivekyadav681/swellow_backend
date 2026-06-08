package com.elsewhere.swellow.wallet;

import com.elsewhere.swellow.user.User;
import com.elsewhere.swellow.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserService userService;

    public WalletService(WalletRepository walletRepository, UserService userService) {
        this.walletRepository = walletRepository;
        this.userService = userService;
    }

    public Wallet getCurrentUserWallet() {
        User user = userService.getCurrentUser();
        return walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for current user"));
    }

    public BalanceResponse getBalances() {
        Wallet wallet = getCurrentUserWallet();
        return new BalanceResponse(wallet.getCashBalance(), wallet.getSwlBalance());
    }

    @Transactional
    public void updateBalancesForTransfer(Long senderWalletId, Long receiverWalletId, BigDecimal amount) {
        Wallet sender = walletRepository.findById(senderWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));
        Wallet receiver = walletRepository.findById(receiverWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        if (sender.getSwlBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient SWL balance");
        }

        sender.setSwlBalance(sender.getSwlBalance().subtract(amount));
        receiver.setSwlBalance(receiver.getSwlBalance().add(amount));

        walletRepository.save(sender);
        walletRepository.save(receiver);
    }

    @Transactional
    public void creditMiningReward(Long walletId, BigDecimal reward) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        wallet.setSwlBalance(wallet.getSwlBalance().add(reward));
        walletRepository.save(wallet);
    }

    @Transactional
    public void updateBalancesForBuy(Long walletId, BigDecimal cashCost, BigDecimal swlAmount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        if (wallet.getCashBalance().compareTo(cashCost) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance");
        }
        wallet.setCashBalance(wallet.getCashBalance().subtract(cashCost));
        wallet.setSwlBalance(wallet.getSwlBalance().add(swlAmount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void updateBalancesForSell(Long walletId, BigDecimal cashEarned, BigDecimal swlAmount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        if (wallet.getSwlBalance().compareTo(swlAmount) < 0) {
            throw new IllegalArgumentException("Insufficient SWL balance");
        }
        wallet.setSwlBalance(wallet.getSwlBalance().subtract(swlAmount));
        wallet.setCashBalance(wallet.getCashBalance().add(cashEarned));
        walletRepository.save(wallet);
    }
}
