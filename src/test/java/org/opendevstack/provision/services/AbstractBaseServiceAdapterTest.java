package org.opendevstack.provision.services;

import java.io.IOException;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.verification.VerificationMode;
import org.opendevstack.provision.util.RestClientCallArgumentMatcher;
import org.opendevstack.provision.util.rest.RestClient;
import org.opendevstack.provision.util.rest.RestClientMockHelper;

/**
 * Base class for all AdapterTests the provide basic functionality for mocking calls to {@link
 * RestClient}
 */
public abstract class AbstractBaseServiceAdapterTest {

  @Mock RestClient restClient;

  RestClientMockHelper mockHelper;

  @Before
  public void beforeTest() {
    MockitoAnnotations.initMocks(this);
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
