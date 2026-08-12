package com.github.victorrentea.livecoding.ux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CopyNiceGitHubUrlTest {

    @Test
    fun httpsRemote() {
        assertEquals("https://github.com/acme/orders", gitHubWebUrl("https://github.com/acme/orders.git"))
    }

    @Test
    fun httpsRemoteWithoutDotGit() {
        assertEquals("https://github.com/acme/orders", gitHubWebUrl("https://github.com/acme/orders"))
    }

    @Test
    fun httpsRemoteWithUserInfoAndTrailingSlash() {
        assertEquals("https://github.com/acme/orders", gitHubWebUrl("https://someone@github.com/acme/orders/"))
    }

    @Test
    fun scpLikeSshRemote() {
        assertEquals("https://github.com/acme/orders", gitHubWebUrl("git@github.com:acme/orders.git"))
    }

    @Test
    fun sshUrlRemoteWithPort() {
        assertEquals("https://github.com/acme/orders", gitHubWebUrl("ssh://git@github.com:22/acme/orders.git"))
    }

    @Test
    fun gitHubEnterpriseRemote() {
        assertEquals("https://github.acme.io/team/orders", gitHubWebUrl("git@github.acme.io:team/orders.git"))
    }

    @Test
    fun nonGitHubRemoteIsIgnored() {
        assertNull(gitHubWebUrl("git@gitlab.com:acme/orders.git"))
        assertNull(gitHubWebUrl("https://bitbucket.org/acme/orders.git"))
    }

    @Test
    fun fileOnBranch() {
        assertEquals(
            "https://github.com/acme/orders/blob/main/docs/CLAUDE.md",
            niceGitHubUrl(listOf("https://github.com/acme/orders.git"), "main", "docs/CLAUDE.md", isDirectory = false)
        )
    }

    @Test
    fun directoryUsesTree() {
        assertEquals(
            "https://github.com/acme/orders/tree/main/docs",
            niceGitHubUrl(listOf("https://github.com/acme/orders.git"), "main", "docs", isDirectory = true)
        )
    }

    @Test
    fun repositoryRootUsesTree() {
        assertEquals(
            "https://github.com/acme/orders/tree/main",
            niceGitHubUrl(listOf("https://github.com/acme/orders.git"), "main", null, isDirectory = true)
        )
    }

    @Test
    fun branchWithSlashesKeepsItsSlashes() {
        assertEquals(
            "https://github.com/acme/orders/blob/feature/nice-urls/README.md",
            niceGitHubUrl(listOf("https://github.com/acme/orders.git"), "feature/nice-urls", "README.md", false)
        )
    }

    @Test
    fun spacesInThePathAreEncoded() {
        assertEquals(
            "https://github.com/acme/orders/blob/main/my%20docs/read%20me.md",
            niceGitHubUrl(listOf("https://github.com/acme/orders.git"), "main", "my docs/read me.md", false)
        )
    }

    @Test
    fun firstGitHubRemoteWins() {
        assertEquals(
            "https://github.com/acme/orders/blob/main/pom.xml",
            niceGitHubUrl(
                listOf("git@gitlab.com:acme/orders.git", "https://github.com/acme/orders.git"),
                "main", "pom.xml", false
            )
        )
    }

    @Test
    fun noGitHubRemoteMeansNoUrl() {
        assertNull(niceGitHubUrl(listOf("git@gitlab.com:acme/orders.git"), "main", "pom.xml", false))
    }
}
