package com.elsewhere.swellow.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "market_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketState {

    @Id
    private Long id; // Always 1

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal totalSupply;

    @Column(nullable = false)
    private Instant lastUpdated;
}
