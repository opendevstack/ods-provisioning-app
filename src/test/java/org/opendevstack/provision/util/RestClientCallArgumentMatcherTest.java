package org.opendevstack.provision.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.opendevstack.provision.util.RestClientCallArgumentMatcher.matchesClientCall;
import static org.opendevstack.provision.util.rest.RestClientCall.get;

import org.junit.Test;
import org.opendevstack.provision.util.rest.RestClientCall;

/** @author Stefan Lack */
public class RestClientCallArgumentMatcherTest {

  @Test
  public void matchesURLWithEquals() {
    String url = "https://www.opitz-consulting.com/index.html";
    RestClientCall given = get().url(url);
    assertThatMatches(given, matchesClientCall().url(url));
  }

  @Test
  public void matchesURLWithMatcher() {
    String url = "https://www.opitz-consulting.com/index.html";
    RestClientCall given = get().url(url);
    assertThatMatches(given, matchesClientCall().url(containsString("opitz")));
  }

  @Test
  public void matchesQueryParams() {
    RestClientCall given = get().queryParam("p1", "v1")
        .queryParam("p2","v2");
    assertThatMatches(given,matchesClientCall().queryParam("p1","v1"));
  }

  @Test
  public void capturedValueIsReturnedIfMatcherMatches() {

    ValueCaptor<String> urlValueHolder = new ValueCaptor<>();
    ValueCaptor<Object> bodyValueHolder = new ValueCaptor<>();

    RestClientCall given = get().url("https://www.opitz-consulting.com/index.html").body(42);
    assertThatMatches(
        given,
        matchesClientCall()
            .url(containsString("opitz"))
            .urlCaptor(urlValueHolder)
            .bodyEqualTo(given.getBody())
            .bodyCaptor(bodyValueHolder));

    assertThat(
        "given value is returned for url",
        urlValueHolder.getValues().get(0),
        equalTo(given.getUrl()));
    assertThat(
        "given value is returned for body",
        bodyValueHolder.getValues().get(0),
        equalTo(given.getBody()));
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

    assertThat(
        "matcher is expected to not match, but it matched", matcher.matches(given), equalTo(false));

    // check that no value if captured at all, because at least one property is not valid according
    // to matcher

    assertThat("expected no value was captured for url", urlValueHolder.getValues(), hasSize(0));

    assertThat("expected no value was captured for body", bodyValueHolder.getValues(), hasSize(0));
  }

  public void assertThatMatches(RestClientCall given, RestClientCallArgumentMatcher matcher) {
    boolean matches = matcher.matches(given);
    //assertThat(String.format("Matcher %s should match",matcher), matches, equalTo(true));
    assertThat("PRÜFEN "+matcher.toString(), matches, equalTo(true));
  }
}
