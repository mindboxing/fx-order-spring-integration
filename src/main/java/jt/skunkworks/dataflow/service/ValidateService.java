package jt.skunkworks.dataflow.service;

import jt.skunkworks.dataflow.message.FxOrder;
import jt.skunkworks.dataflow.service.client.FundClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateService {

    @NonNull
    private FundClient fundClient;

    public FxOrder validate(FxOrder input) {
        log.debug("!!! ValidateService " + input);
        boolean check = fundClient.sufficientFundCheck(input);
        input.setFunded(check);
        return input;
    }
}
