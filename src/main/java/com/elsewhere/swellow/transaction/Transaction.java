package com.elsewhere.swellow.transaction;

import com.elsewhere.swellow.blockchain.Block;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderWalletId; // null for system minting / buys
    private Long receiverWalletId; // null for system burning / sells

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal amount;

    @Column(length = 2048)
    private String signature; // Base64 signature

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    @JsonIgnore
    private Block block;

    @Column(nullable = false)
    private Instant timestamp;
}
