package jt.skunkworks.dataflow.service.client;

import jt.skunkworks.dataflow.config.AppConfigProps;
import jt.skunkworks.dataflow.message.FxOrder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class FundClient {

    @NonNull
    private final RestTemplate restTemplate;
    @NonNull
    private final AppConfigProps configProps;

    public boolean sufficientFundCheck(FxOrder order) {
        System.out.println("!!! sufficientFundCheck ");
        String uri = String.format("%s/fund/{accountId}/fundSufficientCheck", configProps.getFundServiceUrl());
        var request = new SufficientFundCheckRequestBuilder(order).build();
        ResponseEntity<SufficientFundCheckResponse> response = restTemplate.postForEntity(
                uri,
                request,
                SufficientFundCheckResponse.class,
                order.getAccountId());
        return response.getBody().getResult() == FundCheckResult.APPROVED;
    }
}

