package com.evbox.everon.ocpp.mock.match;

import com.evbox.everon.ocpp.mock.expect.ExpectedCount;
import com.evbox.everon.ocpp.simulator.message.Call;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents an expectation of the request.
 */
public class RequestMatcher extends Matcher<Call> {

    private final Function<Call, String> expectedResponse;

    public RequestMatcher(Predicate<Call> expectedRequest, ExpectedCount expectedCount, Function<Call, String> expectedResponse) {
        super(expectedRequest, expectedCount);
        this.expectedResponse = expectedResponse;
    }

    /**
     * Return expected response associated with the request.
     *
     * @return expected response.
     */
    public Function<Call, String> getExpectedResponse() {
        return expectedResponse;
    }

}
