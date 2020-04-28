import groovy.text.SimpleTemplateEngine

/**
 * Test rendering a groovy template
 * @param gitRepo
 * @param username
 * @param password
 * @return
 */
def call(String input, Map binding) {
    def engine = new SimpleTemplateEngine()
    def template = engine.createTemplate(input).make(binding)
    return template.toString()
}