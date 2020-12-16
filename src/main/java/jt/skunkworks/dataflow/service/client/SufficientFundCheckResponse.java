package jt.skunkworks.dataflow.service.client;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SufficientFundCheckResponse {
    private FundCheckResult result;
}
