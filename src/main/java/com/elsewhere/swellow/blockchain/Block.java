package com.elsewhere.swellow.blockchain;

import com.elsewhere.swellow.transaction.Transaction;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long blockIndex;

    @Column(nullable = false)
    private String previousHash;

    @Column(nullable = false)
    private String currentHash;

    @Column(nullable = false)
    private Long timestamp; // Epoch milliseconds

    @OneToMany(mappedBy = "block", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();
}
