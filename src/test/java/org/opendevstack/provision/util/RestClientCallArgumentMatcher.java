package org.opendevstack.provision.util;

import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.mockito.ArgumentMatcher;
import org.opendevstack.provision.util.rest.RestClientCall;
import org.springframework.http.HttpMethod;

/**
 * Mockito ArgumentMatcher implementation for {@link RestClientCall} instances.
 *
 * <p>Provides a fluent api to specify features that are matched from the matcher. The internal
 * matcher implementation is done via hamcrest's {@link FeatureMatcher} class.
 *
 * <p>Beside matching, the {@link RestClientCallArgumentMatcher} also provides a very simple captor
 * implementation, wich allows to read out the captures values. Values are only captured, in case
 * the matcher matches.
 *
 * <p>See RestClientCallArgumentMatcher#bodyCaptor and RestClientCallArgumentMatcher#urlCaptor
 *
 * @author Stefan Lack
 */
public class RestClientCallArgumentMatcher implements ArgumentMatcher<RestClientCall> {

  private final List<Pair<FeatureMatcher, Function<RestClientCall, ?>>> featureMatcherList;
  private final List<Pair<FeatureMatcher, Object>> invalidMatcherList;
  private final Map<Function<RestClientCall, Object>, ValueCaptor<?>> captorMap;
  private Description mismatchDescription;

  protected RestClientCallArgumentMatcher() {
    featureMatcherList = new ArrayList<>();
    invalidMatcherList = new ArrayList<>();
    captorMap = new HashMap<>();
  }

  public static RestClientCallArgumentMatcher matchesClientCall() {
    return new RestClientCallArgumentMatcher();
  }

  @Override
  public boolean matches(RestClientCall argument) {
    invalidMatcherList.clear();
    mismatchDescription = new StringDescription();
    for (Pair<FeatureMatcher, Function<RestClientCall, ?>> pair : featureMatcherList) {
      FeatureMatcher featureMatcher = pair.getLeft();
      featureMatcher.describeMismatch(argument, mismatchDescription);

      if (!featureMatcher.matches(argument)) {
        Function<RestClientCall, ?> valueExtractor = pair.getRight();
        Object apply = argument == null ? null : valueExtractor.apply(argument);
        invalidMatcherList.add(Pair.of(featureMatcher, apply));
      }
    }
    boolean isValid = invalidMatcherList.size() == 0;
    if (isValid) {
      captureArgumentValues(argument);
    }
    return isValid;
  }

  /**
   * For every configured captor (defined in RestClientCallArgumentMatcher#captorMap), the value is
   * read out from the given argument and stored inside the valueCaptor
   *
   * @param argument the argument where the values should be captured from
   */
  private void captureArgumentValues(RestClientCall argument) {
    for (Entry<Function<RestClientCall, Object>, ValueCaptor<?>> entry : captorMap.entrySet()) {
      Function<RestClientCall, Object> function = entry.getKey();
      ValueCaptor<Object> valueHolder = (ValueCaptor<Object>) entry.getValue();

      Object value = function.apply(argument);
      valueHolder.addValue(value);
    }
  }

  public String toString() {
    if (invalidMatcherList.isEmpty()) {
      String validMatchers =
          featureMatcherList.stream()
              .map(Pair::getLeft)
              .map(BaseMatcher::toString)
              .collect(Collectors.joining(","));

      return "All matchers suceeded: " + validMatchers;
    }

    Description description = new StringDescription();
    // result.append("A ClientCall with the following properties:");
    for (Pair<FeatureMatcher, Object> pair : invalidMatcherList) {
      FeatureMatcher invalidMatcher = pair.getLeft();

      description.appendText("Expecting '");
      invalidMatcher.describeTo(description);
      description.appendText("', but got values:").appendValue(pair.getRight());
    }

    return description.toString();
  }

  public RestClientCallArgumentMatcher url(Matcher<String> matcher) {
    addMatcher(matcher, "url", RestClientCall::getUrl);
    return this;
  }

  public RestClientCallArgumentMatcher url(String value) {
    return url(equalTo(value));
  }

  public RestClientCallArgumentMatcher urlCaptor(ValueCaptor<String> captor) {
    captorMap.put(RestClientCall::getUrl, captor);
    return this;
  }

  public RestClientCallArgumentMatcher bodyMatches(Matcher<Object> matcher) {
    addMatcher(matcher, "body", RestClientCall::getBody);
    return this;
  }

  public RestClientCallArgumentMatcher bodyEqualTo(Object value) {
    return bodyMatches(equalTo(value));
  }

  public <T> RestClientCallArgumentMatcher bodyCaptor(ValueCaptor<T> captor) {
    captorMap.put(RestClientCall::getBody, captor);
    return this;
  }

  public RestClientCallArgumentMatcher method(HttpMethod method) {
    addMatcher(equalTo(method), "method", RestClientCall::getMethod);
    return this;
  }

  public RestClientCallArgumentMatcher queryParam(String param, String value) {
    addMatcher(Matchers.hasEntry(param, value), "queryParams", RestClientCall::getQueryParams);
    return this;
  }

  public RestClientCallArgumentMatcher returnType(Class value) {
    return returnType(equalTo(value));
  }

  public RestClientCallArgumentMatcher returnType(Matcher<Object> matcher) {
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
    addMatcher(featureMatcher, valueExtractor);
  }

  protected <T> void addMatcher(
      FeatureMatcher featureMatcher, Function<RestClientCall, T> valueExtractor) {
    featureMatcherList.add(Pair.of(featureMatcher, valueExtractor));
  }
}
