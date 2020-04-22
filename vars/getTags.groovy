/**
 * Force a fetch of tags.
 * This must be called within a valid withCredentials {} block to work.
 */
def call(Map config) {
    sh 'git fetch --all --tags -p -P'
}