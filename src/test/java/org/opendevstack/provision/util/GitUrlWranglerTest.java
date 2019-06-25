package org.opendevstack.provision.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import org.junit.Test;

public class GitUrlWranglerTest
{
    private GitUrlWrangler urlWrangler = new GitUrlWrangler();

    @Test
    public void replacesUsernameWithTechnicalUser()
            throws UnsupportedEncodingException
    {
        String href = "https://opendevstack.developer@bitbucket.opendevstack.org";
        String result = urlWrangler.buildGitUrl(
                "opendevstack.developer", "cd_user", href);
        assertEquals("https://cd_user@bitbucket.opendevstack.org",
                result);
    }

    @Test
    public void insertsTechnicalUserIfUserAbsent()
            throws UnsupportedEncodingException
    {
        String href = "https://bitbucket.opendevstack.org";
        String result = urlWrangler.buildGitUrl(
                "opendevstack.developer", "cd_user", href);
        assertEquals("https://cd_user@bitbucket.opendevstack.org",
                result);
    }

    @Test
    public void handlesAlreadyUrlEncodedUsernameCorrectly()
            throws UnsupportedEncodingException
    {
        String href = "https://opendevstack+developer@bitbucket.opendevstack.org";
        String result = urlWrangler.buildGitUrl(
                "opendevstack+developer", "cd_user", href);
        assertEquals("https://cd_user@bitbucket.opendevstack.org",
                result);
    }

    @Test
    public void leavesSshUrlsUnmodified()
            throws UnsupportedEncodingException
    {
        String cloneUrl = "ssh://git@bitbucket.opendevstack.org";
        String result = urlWrangler.buildGitUrl(
                "opendevstack.developer", "cd_user", cloneUrl);
        assertEquals(cloneUrl, result);
    }

    @Test
    public void urlEncodedStringsAreDetected()
    {
        assertTrue(urlWrangler
                .followsUrlEncodingScheme("opendevstack+developer"));
        assertTrue(urlWrangler.followsUrlEncodingScheme(
                "The+string+%C3%BC%40foo-bar"));
    }

    @Test
    public void notCorrectlyEncodedStringsAreDetected()
    {
        assertFalse(urlWrangler
                .followsUrlEncodingScheme("opendevstack developer"));
        assertFalse(urlWrangler
                .followsUrlEncodingScheme("opendevstack@developer"));

    }

}
