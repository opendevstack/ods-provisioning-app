package org.opendevstack.provision.util;

import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.springframework.http.HttpMethod;

public class ClientCallArgumentMatcher implements ArgumentMatcher<RestClientCall> {

  private final List<FeatureMatcher> featureMatcherList;
  private final List<FeatureMatcher> invalidMatcherList;

  protected ClientCallArgumentMatcher() {
    featureMatcherList = new ArrayList<>();
    invalidMatcherList = new ArrayList<>();
  }

  public static ClientCallArgumentMatcher matchesClientCall() {
    return new ClientCallArgumentMatcher();
  }

  @Override
  public boolean matches(RestClientCall argument) {
    invalidMatcherList.clear();
    for (FeatureMatcher featureMatcher : featureMatcherList) {
      if (!featureMatcher.matches(argument)) {
        invalidMatcherList.add(featureMatcher);
      }
    }
    return invalidMatcherList.size() == 0;
  }

  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("A ClientCall with the following properties:");

    for (FeatureMatcher invalidMatcher : invalidMatcherList) {
      Description description = new StringDescription();
      invalidMatcher.describeTo(description);
      result.append(description.toString());
    }
    return result.toString();
  }

  public ClientCallArgumentMatcher url(Matcher<String> matcher) {
    addMatcher(matcher, "url", RestClientCall::getUrl);
    return this;
  }

  public ClientCallArgumentMatcher url(String value) {
    return url(equalTo(value));
  }

  public ClientCallArgumentMatcher bodyMatches(Matcher<Object> matcher) {
    addMatcher(matcher, "body", RestClientCall::getBody);
    return this;
  }

  public ClientCallArgumentMatcher bodyEqualTo(Object value) {
    return bodyMatches(equalTo(value));
  }

  public ClientCallArgumentMatcher method(HttpMethod method) {
    addMatcher(equalTo(method), "method", RestClientCall::getMethod);
    return this;
  }

  public ClientCallArgumentMatcher returnType(Class value) {
    return returnType(equalTo(value));
  }

  public ClientCallArgumentMatcher returnType(Matcher<Object> matcher) {
    addMatcher(matcher, "returnType", RestClientCall::getReturnType);
    return this;
  }

  private <T> void addMatcher(
      Matcher<T> matcher, String featureName, Function<RestClientCall, T> valueExtractor) {
    FeatureMatcher<RestClientCall, T> featureMatcher =
        new FeatureMatcher<RestClientCall, T>(matcher, featureName, featureName) {
          public T featureValueOf(RestClientCall actual) {
            return valueExtractor.apply(actual);
          }
        };
    addMatcher(featureMatcher);
  }

  private void addStringMatcher(
      Matcher<String> matcher,
      String featureName,
      Function<RestClientCall, String> valueExtractor) {
    FeatureMatcher<RestClientCall, String> featureMatcher =
        new FeatureMatcher<RestClientCall, String>(matcher, featureName, featureName) {
          public String featureValueOf(RestClientCall actual) {
            return valueExtractor.apply(actual);
          }
        };
    addMatcher(featureMatcher);
  }

  protected void addMatcher(FeatureMatcher featureMatcher) {
    featureMatcherList.add(featureMatcher);
  }

  public ClientCallArgumentMatcher bodyCapture(ArgumentCaptor captor) {
    // TODO implement captor, if possible
    return this;
  }
}
