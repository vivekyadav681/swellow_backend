package com.elsewhere.swellow.blockchain;

import com.elsewhere.swellow.common.CryptoUtil;
import com.elsewhere.swellow.common.HashUtil;
import com.elsewhere.swellow.transaction.Transaction;
import com.elsewhere.swellow.wallet.Wallet;
import com.elsewhere.swellow.wallet.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BlockchainService {

    private final BlockRepository blockRepository;
    private final WalletRepository walletRepository;

    public BlockchainService(BlockRepository blockRepository, WalletRepository walletRepository) {
        this.blockRepository = blockRepository;
        this.walletRepository = walletRepository;
    }

    public List<Block> getBlocks() {
        return blockRepository.findAllByOrderByBlockIndexAsc();
    }

    public Optional<Block> getBlockById(Long id) {
        return blockRepository.findById(id);
    }

    public Optional<Block> getLatestBlock() {
        return blockRepository.findFirstByOrderByBlockIndexDesc();
    }

    public static String getTransactionDataString(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return "";
        }
        
        // Sort transactions by database ID to ensure deterministic order
        List<Transaction> sortedTxs = new ArrayList<>(transactions);
        sortedTxs.sort((t1, t2) -> {
            if (t1.getId() == null && t2.getId() == null) return 0;
            if (t1.getId() == null) return -1;
            if (t2.getId() == null) return 1;
            return t1.getId().compareTo(t2.getId());
        });

        StringBuilder sb = new StringBuilder();
        for (Transaction tx : sortedTxs) {
            sb.append(tx.getId() == null ? "null" : tx.getId())
              .append(":")
              .append(tx.getSenderWalletId() == null ? "null" : tx.getSenderWalletId())
              .append(":")
              .append(tx.getReceiverWalletId() == null ? "null" : tx.getReceiverWalletId())
              .append(":")
              .append(tx.getAmount().setScale(8, RoundingMode.HALF_UP).toPlainString())
              .append(":")
              .append(tx.getSignature())
              .append(":")
              .append(tx.getTimestamp().toEpochMilli())
              .append(",");
        }
        return sb.toString();
    }

    public static String calculateHashForBlock(Long blockIndex, Long timestamp, List<Transaction> transactions, String previousHash) {
        String data = blockIndex + "-" + timestamp + "-" + getTransactionDataString(transactions) + "-" + previousHash;
        return HashUtil.applySha256(data);
    }

    public static String calculateBlockHash(Block block) {
        return calculateHashForBlock(block.getBlockIndex(), block.getTimestamp(), block.getTransactions(), block.getPreviousHash());
    }

    public boolean validateChain() {
        List<Block> chain = getBlocks();
        if (chain.isEmpty()) {
            return true;
        }

        for (int i = 0; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);

            // 1. Verify block current hash correctness
            String calculatedHash = calculateBlockHash(currentBlock);
            if (!calculatedHash.equals(currentBlock.getCurrentHash())) {
                return false;
            }

            // 2. Verify linkage to previous block
            if (i > 0) {
                Block previousBlock = chain.get(i - 1);
                if (!currentBlock.getPreviousHash().equals(previousBlock.getCurrentHash())) {
                    return false;
                }
            } else {
                // Genesis block previous hash check
                if (!"0".equals(currentBlock.getPreviousHash())) {
                    return false;
                }
            }

            // 3. Verify signatures of transactions inside the block
            for (Transaction tx : currentBlock.getTransactions()) {
                if (tx.getSenderWalletId() == null || tx.getReceiverWalletId() == null) {
                    return false;
                }

                Wallet senderWallet = walletRepository.findById(tx.getSenderWalletId()).orElse(null);
                if (senderWallet == null) {
                    return false;
                }

                if (senderWallet.getWalletType() == com.elsewhere.swellow.wallet.WalletType.TREASURY) {
                    // Treasury signature rule
                    if (!"TREASURY_TRANSFER".equals(tx.getSignature())) {
                        return false;
                    }
                } else {
                    // Verify ECDSA signature against sender's public key
                    String receiverVal = tx.getReceiverWalletId().toString();
                    String formattedAmount = tx.getAmount().setScale(8, RoundingMode.HALF_UP).toPlainString();
                    String signedData = tx.getSenderWalletId() + ":" + receiverVal + ":" + formattedAmount + ":" + tx.getTimestamp().toEpochMilli();

                    boolean isSigValid = CryptoUtil.verify(signedData, tx.getSignature(), senderWallet.getPublicKey());
                    if (!isSigValid) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
