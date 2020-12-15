package org.opendevstack.provision.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;
import static org.opendevstack.provision.util.rest.RestClientCall.get;

import org.junit.jupiter.api.Test;
import org.opendevstack.provision.util.rest.RestClientCall;

public class RestClientCallArgumentMatcherTest {

  @Test
  public void matchesURLWithEquals() {
    String url = "https://www.opitz-consulting.com/index.html";
    RestClientCall given = get().url(url);
    assertTrue(matchesClientCall().url(url).matches(given));
  }

  @Test
  public void matchesURLWithMatcher() {
    String url = "https://www.opitz-consulting.com/index.html";
    RestClientCall given = get().url(url);
    assertTrue(matchesClientCall().url(containsString("opitz")).matches(given));
  }

  @Test
  public void matchesQueryParams() {
    RestClientCall given = get().queryParam("p1", "v1").queryParam("p2", "v2");
    assertTrue(matchesClientCall().queryParam("p1", "v1").matches(given));
  }

  @Test
  public void capturedValueIsReturnedIfMatcherMatches() {

    ValueCaptor<String> urlValueHolder = new ValueCaptor<>();
    ValueCaptor<Object> bodyValueHolder = new ValueCaptor<>();

    RestClientCall given = get().url("https://www.opitz-consulting.com/index.html").body(42);
    assertTrue(
        matchesClientCall()
            .url(containsString("opitz"))
            .urlCaptor(urlValueHolder)
            .bodyEqualTo(given.getBody())
            .bodyCaptor(bodyValueHolder)
            .matches(given));

    assertEquals(
        given.getUrl(), urlValueHolder.getValues().get(0), "given value is returned for url");
    assertEquals(
        given.getBody(), bodyValueHolder.getValues().get(0), "given value is returned for body");
  }

  @Test
  public void capturedValueIsNotReturnedIfMatcherDoesNotMatches() {

    ValueCaptor<String> urlValueHolder = new ValueCaptor<>();
    ValueCaptor<Object> bodyValueHolder = new ValueCaptor<>();

    RestClientCall given = get().url("https://www.opitz-consulting.com/index.html").body(42);

    RestClientCallArgumentMatcher matcher =
        matchesClientCall()
            .url(containsString("opitz")) // valid matcher
            .urlCaptor(urlValueHolder)
            .bodyEqualTo("the answer to life the universe and everything") // does not match
            .bodyCaptor(bodyValueHolder);

    assertFalse(matcher.matches(given), "matcher is expected to not match, but it matched");

    // check that no value if captured at all, because at least one property is not valid according
    // to matcher

    assertTrue(urlValueHolder.getValues().isEmpty(), "expected no value was captured for url");

    assertTrue(bodyValueHolder.getValues().isEmpty(), "expected no value was captured for body");
  }
}
