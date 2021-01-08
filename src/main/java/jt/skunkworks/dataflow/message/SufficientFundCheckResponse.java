package jt.skunkworks.dataflow.message;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SufficientFundCheckResponse {
    private FundCheckResult result;
}
