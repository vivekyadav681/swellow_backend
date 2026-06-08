package com.elsewhere.swellow.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {
    private BigDecimal cashBalance;
    private BigDecimal swlBalance;
}
