/**
 * Force a fetch of tags.
 * This must be called within a valid withCredentials {} block to work.
 * Provided to workaround Jenkins builds that can't fetch tags for some reason.
 */
def call(String gitRepo, String username, String password) {
    sh "git fetch https://${username}:${password}@${gitRepo} --tags -p -P"
}