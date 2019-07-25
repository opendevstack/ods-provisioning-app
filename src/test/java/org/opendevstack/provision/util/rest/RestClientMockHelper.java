package org.opendevstack.provision.util.rest;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.verification.VerificationMode;
import org.opendevstack.provision.util.RestClientCallArgumentMatcher;

public class RestClientMockHelper {

  private final RestClient restClient;

  public RestClientMockHelper(RestClient restClient) {

    this.restClient = restClient;
  }

  /**
   * Mock call of client.execute. The caller may specify further mocking behaviour by calling
   * additional methods on the returned OngoingStubbing object.
   *
   * @param call arguments that are passed to method <em>execute</em> as argThat - matcher.
   * @return
   * @throws IOException
   */
  public OngoingStubbing<Object> mockExecute(RestClientCallArgumentMatcher call)
      throws IOException {
    return when(restClient.execute(argThat(call)));
  }

  /**
   * Verifies that execute is called exactly one time with a argument that matches the specified
   * matcher.
   *
   * @param wantedArgument a {@link RestClientCallArgumentMatcher} to specify the expected argument
   * @throws IOException
   */
  public void verifyExecute(RestClientCallArgumentMatcher wantedArgument) throws IOException {
    int wantedNumberOfInvocations = 1;
    verifyExecute(wantedArgument, wantedNumberOfInvocations);
  }

  /**
   * Verifies that execute is called exactly one time with a argument that matches the specified
   * matcher.
   *
   * @param wantedArgument a {@link RestClientCallArgumentMatcher} to specify the expected argument
   * @param wantedNumberOfInvocations expected number of invocations
   * @throws IOException
   */
  public void verifyExecute(
      RestClientCallArgumentMatcher wantedArgument, int wantedNumberOfInvocations)
      throws IOException {
    VerificationMode times = Mockito.times(wantedNumberOfInvocations);
    verifyExecute(wantedArgument, times);
  }

  /**
   * Verifies that execute is called exactly one time with a argument that matches the specified
   * matcher.
   *
   * @param wantedArgument a {@link RestClientCallArgumentMatcher} to specify the expected argument
   * @param times the {@link VerificationMode}
   * @throws IOException
   */
  public void verifyExecute(RestClientCallArgumentMatcher wantedArgument, VerificationMode times)
      throws IOException {
    Mockito.verify(restClient, times).execute(argThat(wantedArgument));
  }
}
