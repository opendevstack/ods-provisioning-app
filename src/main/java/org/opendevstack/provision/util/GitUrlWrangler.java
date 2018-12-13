package org.opendevstack.provision.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class GitUrlWrangler {

  /**
   * Checks, if a string already follows url encoding guidelines.
   * This is tested against the contained characters
   * @param toTest the string that should be tested for conformance
   * @return true, if string conforms the url encoding rules
   */
  boolean followsUrlEncodingScheme(String toTest) {
    return Pattern.compile("[a-zA-Z0-9%+\\-]+?").matcher(toTest).matches();
  }

  /**
   * Returns a http git url containing the technical user for accessing git
   * @param username the username that created the repo. Is returned as part of the URL by bitbucket for versions < 5.13.0
   * @param technicalUser the technical user used for accessing the repo
   * @param cloneURL the clone url
   * @return a suitable clone url, containing the technical user
   * @throws UnsupportedEncodingException
   */
  public String buildGitUrl(String username, String technicalUser, String cloneURL)
      throws UnsupportedEncodingException {
    String result=cloneURL;
    if(cloneURL.startsWith("http")) {
      String urlEncodedUsername = followsUrlEncodingScheme(username)? username : URLEncoder
          .encode(username, "UTF-8");
      String urlEncodedTechnicalUser = followsUrlEncodingScheme(technicalUser)? technicalUser : URLEncoder.encode(technicalUser, "UTF-8");
      result = cloneURL.replace(urlEncodedUsername, urlEncodedTechnicalUser);
      if (result.equals(cloneURL)) {
        result = Pattern.compile("(https?://)(.*)").matcher(cloneURL).replaceAll("$1"+urlEncodedTechnicalUser+"@$2");
      }
    }
    return result;
  }
}
