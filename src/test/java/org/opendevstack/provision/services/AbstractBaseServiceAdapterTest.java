package org.opendevstack.provision.services;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.verification.VerificationMode;
import org.opendevstack.provision.util.ClientCallArgumentMatcher;
import org.opendevstack.provision.util.rest.RestClient;

/**
 * Base class for all AdapterTests the provide basic functionality for mocking calls to {@link
 * RestClient}
 */
public abstract class AbstractBaseServiceAdapterTest {

  @Mock RestClient client2;

  /**
   * Mock call of client.execute. The caller may specify further mocking behaviour by calling
   * additional methods on the returned OngoingStubbing object.
   *
   * @param call arguments that are passed to method <em>execute</em> as argThat - matcher.
   * @return
   * @throws IOException
   */
  public OngoingStubbing<Object> mockExecute(ClientCallArgumentMatcher call) throws IOException {
    return when(client2.execute(argThat(call)));
  }

  /**
   * Verifies that execute is called exactly one time with a argument that matches the specified
   * matcher.
   *
   * @param wantedArgument a {@link ClientCallArgumentMatcher} to specify the expected argument
   * @throws IOException
   */
  public void verifyExecute(ClientCallArgumentMatcher wantedArgument) throws IOException {
    int wantedNumberOfInvocations = 1;
    verifyExecute(wantedArgument, wantedNumberOfInvocations);
  }

  /**
   * Verifies that execute is called exactly one time with a argument that matches the specified
   * matcher.
   *
   * @param wantedArgument a {@link ClientCallArgumentMatcher} to specify the expected argument
   * @param wantedNumberOfInvocations expected number of invocations
   * @throws IOException
   */
  public void verifyExecute(ClientCallArgumentMatcher wantedArgument, int wantedNumberOfInvocations)
      throws IOException {
    VerificationMode times = Mockito.times(wantedNumberOfInvocations);
    verifyExecute(wantedArgument, times);
  }

  /**
   * Verifies that execute is called exactly one time with a argument that matches the specified
   * matcher.
   *
   * @param wantedArgument a {@link ClientCallArgumentMatcher} to specify the expected argument
   * @param times the {@link VerificationMode}
   * @throws IOException
   */
  public void verifyExecute(ClientCallArgumentMatcher wantedArgument, VerificationMode times)
      throws IOException {
    Mockito.verify(client2, times).execute(argThat(wantedArgument));
  }
}
