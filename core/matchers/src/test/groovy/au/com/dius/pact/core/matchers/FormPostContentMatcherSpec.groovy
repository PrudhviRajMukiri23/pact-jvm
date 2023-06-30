package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import spock.lang.Specification

@SuppressWarnings(['PrivateFieldCouldBeFinal', 'LineLength'])
class FormPostContentMatcherSpec extends Specification {

  private FormPostContentMatcher matcher
  private MatchingContext context

  def setup() {
    matcher = new FormPostContentMatcher()
    context = new MatchingContext(new MatchingRuleCategory('body'), true)
  }

  def 'returns no mismatches - when the expected body is missing'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:
    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.missing()
  }

  def 'returns no mismatches - when the expected body and actual bodies are empty'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:
    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()
  }

  def 'returns no mismatches - when the expected body and actual bodies are equal'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:
    actualBody = OptionalBody.body('a=b&c=d'.bytes)
    expectedBody = OptionalBody.body('a=b&c=d'.bytes)
  }

  def 'returns no mismatches - when the actual body has extra keys and we allow unexpected keys'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:
    actualBody = OptionalBody.body('a=b&c=d'.bytes)
    expectedBody = OptionalBody.body('a=b'.bytes)
  }

  def 'returns no mismatches - when the keys are in different order'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:
    actualBody = OptionalBody.body('a=b&c=d'.bytes)
    expectedBody = OptionalBody.body('c=d&a=b'.bytes)
  }

  def 'returns mismatches - when the expected body contains keys that are not in the actual body'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch ==
      ['Expected form post parameter \'c\' but was missing']

    where:
    actualBody = OptionalBody.body('a=b'.bytes)
    expectedBody = OptionalBody.body('a=b&c=d'.bytes)
  }

  def 'returns mismatches - when the expected body contains less values than the actual body'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch ==
      ['Expected form post parameter \'a\' with 1 value(s) but received 2 value(s)']

    where:
    actualBody = OptionalBody.body('a=b&a=c'.bytes)
    expectedBody = OptionalBody.body('a=b'.bytes)
  }

  def 'returns mismatches - when the expected body contains more values than the actual body'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch ==
      ['Expected form post parameter \'c\'[0] with value \'1\' but was \'2\'',
       'Expected form post parameter \'c\'=\'3000\' but was missing']

    where:
    actualBody = OptionalBody.body('a=b&c=2'.bytes)
    expectedBody = OptionalBody.body('c=1&a=b&c=3000'.bytes)
  }

  @SuppressWarnings('LineLength')
  def 'returns mismatches - when the actual body contains keys that are not in the expected body and we do not allow extra keys'() {
    given:
    context = new MatchingContext(new MatchingRuleCategory('body'), false)

    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch ==
      ['Received unexpected form post parameter \'a\'=[\'b\']']

    where:
    actualBody = OptionalBody.body('a=b&c=d'.bytes)
    expectedBody = OptionalBody.body('c=d'.bytes)
  }

  def 'returns mismatches - when the expected body is present but there is no actual body'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch ==
      ['Expected a form post body but was missing']

    where:
    actualBody = OptionalBody.missing()
    expectedBody = OptionalBody.body('a=a'.bytes)
  }

  def 'returns mismatches - if the same key is repeated with values in different order'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch ==
      [
        'Expected form post parameter \'a\'[0] with value \'1\' but was \'2\'',
        'Expected form post parameter \'a\'[1] with value \'2\' but was \'1\''
      ]

    where:
    actualBody = OptionalBody.body('a=2&a=1&b=3'.bytes)
    expectedBody = OptionalBody.body('a=1&a=2&b=3'.bytes)
  }

  def 'returns mismatches - if the same key is repeated with values missing'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch ==
      [
        'Expected form post parameter \'a\'=\'3\' but was missing'
      ]

    where:
    actualBody = OptionalBody.body('a=1&a=2'.bytes)
    expectedBody = OptionalBody.body('a=1&a=2&a=3'.bytes)
  }

  def 'returns mismatches - when the actual body contains values that are not the same as the expected body'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch ==
      ['Expected form post parameter \'c\' with value \'d\' but was \'1\'']

    where:
    actualBody = OptionalBody.body('a=b&c=1'.bytes)
    expectedBody = OptionalBody.body('c=d&a=b'.bytes)
  }

  def 'handles delimiters in the values'() {
    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches*.mismatch ==
      ['Expected form post parameter \'c\' with value \'1\' but was \'1=2\'']

    where:
    actualBody = OptionalBody.body('a=b&c=1=2'.bytes)
    expectedBody = OptionalBody.body('c=1&a=b'.bytes)
  }

  def 'delegates to any defined matcher'() {
    given:
    context.matchers.addRule('$.c', TypeMatcher.INSTANCE)

    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:
    actualBody = OptionalBody.body('a=b&c=2'.bytes)
    expectedBody = OptionalBody.body('c=1&a=b'.bytes)
  }

  def 'correctly uses a matcher when there are repeated values'() {
    given:
    context.matchers.addRule('$.c', new RegexMatcher('\\d+'))

    expect:
    matcher.matchBody(expectedBody, actualBody, context).mismatches.empty

    where:
    actualBody = OptionalBody.body('c=1&a=b&c=3000'.bytes)
    expectedBody = OptionalBody.body('a=b&c=2'.bytes)
  }
}
