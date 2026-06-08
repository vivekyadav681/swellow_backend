package com.elsewhere.swellow.wallet;

import com.elsewhere.swellow.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cashBalance;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal swlBalance;

    @Column(nullable = false, length = 2048)
    private String publicKey;

    @Column(nullable = false, length = 2048)
    @JsonIgnore
    private String privateKey;
}
