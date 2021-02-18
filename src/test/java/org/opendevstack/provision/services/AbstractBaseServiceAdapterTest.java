package org.opendevstack.provision.services;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.verification.VerificationMode;
import org.opendevstack.provision.util.RestClientCallArgumentMatcher;
import org.opendevstack.provision.util.rest.RestClient;
import org.opendevstack.provision.util.rest.RestClientMockHelper;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Base class for all AdapterTests the provide basic functionality for mocking calls to {@link
 * RestClient}
 */
public abstract class AbstractBaseServiceAdapterTest {

  @MockBean protected RestClient restClient;

  private RestClientMockHelper mockHelper;

  @BeforeEach
  public void beforeTest() {
    mockHelper = new RestClientMockHelper(restClient);
  }

  /** @see org.opendevstack.provision.util.rest.RestClientMockHelper */
  public OngoingStubbing<Object> mockExecute(RestClientCallArgumentMatcher call)
      throws IOException {
    return mockHelper.mockExecute(call);
  }

  /** @see org.opendevstack.provision.util.rest.RestClientMockHelper */
  public void verifyExecute(RestClientCallArgumentMatcher wantedArgument) throws IOException {
    mockHelper.verifyExecute(wantedArgument);
  }

  /** @see org.opendevstack.provision.util.rest.RestClientMockHelper */
  public void verifyExecute(
      RestClientCallArgumentMatcher wantedArgument, int wantedNumberOfInvocations)
      throws IOException {
    mockHelper.verifyExecute(wantedArgument, wantedNumberOfInvocations);
  }

  /** @see org.opendevstack.provision.util.rest.RestClientMockHelper */
  public void verifyExecute(RestClientCallArgumentMatcher wantedArgument, VerificationMode times)
      throws IOException {
    mockHelper.verifyExecute(wantedArgument, times);
  }
}
