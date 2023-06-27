package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.loader.PactLoader
import au.com.dius.pact.provider.junitsupport.loader.PactSource
import au.com.dius.pact.provider.junitsupport.loader.PactUrl
import au.com.dius.pact.provider.junitsupport.loader.PactUrlLoader
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.InitializationError
import spock.lang.Issue
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@SuppressWarnings('UnusedObject')
class PactRunnerSpec extends Specification {

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  class TestClass {
    @TestTarget
    Target target
  }

  @Provider('Bob')
  class NoSourceTestClass {

  }

  @Provider('Bob')
  @PactUrl(urls = ['http://doesntexist/I%20hope?'])
  class FailsTestClass {

  }

  @Provider('Bob')
  @PactFolder('pacts')
  class NoPactsTestClass {

  }

  @Provider('Bob')
  @PactFolder('pacts')
  @IgnoreNoPactsToVerify
  class NoPactsIgnoredTestClass {

  }

  @Provider('Bob')
  @PactFolder('pacts')
  @PactSource(PactUrlLoader)
  class BothPactSourceAndPactLoaderTestClass {

  }

  @Provider('Bob')
  @PactUrl(urls = ['http://doesnt%20exist/I%20hope?'])
  @IgnoreNoPactsToVerify(ignoreIoErrors = 'true')
  class DoesNotFailsTestClass {

  }

  @Provider('test_provider_combined')
  @PactFolder('pacts')
  class V4TestClass {
    @TestTarget
    Target target
  }

  static class PactLoaderWithConstructorParameter implements PactLoader {

    private final Class clazz

    PactLoaderWithConstructorParameter(Class clazz) {
      this.clazz = clazz
    }

    @Override
    List<Pact> load(String providerName) throws IOException {
      [
        new RequestResponsePact(new au.com.dius.pact.core.model.Provider('Bob'),
          new au.com.dius.pact.core.model.Consumer(), [])
      ]
    }

    @Override
    au.com.dius.pact.core.model.PactSource getPactSource() {
      new UrlSource('url')
    }
  }

  static class PactLoaderWithDefaultConstructor implements PactLoader {

    @Override
    List<Pact> load(String providerName) throws IOException {
      [
        new RequestResponsePact(new au.com.dius.pact.core.model.Provider('Bob'),
          new au.com.dius.pact.core.model.Consumer(), [])
      ]
    }

    @Override
    au.com.dius.pact.core.model.PactSource getPactSource() {
      new UrlSource('url')
    }
  }

  @Provider('Bob')
  @PactSource(PactLoaderWithConstructorParameter)
  class PactLoaderWithConstructorParameterTestClass {
    @TestTarget
    Target target
  }

  @Provider('Bob')
  @PactSource(PactLoaderWithDefaultConstructor)
  class PactLoaderWithDefaultConstructorClass {
    @TestTarget
    Target target
  }

  @Provider('${provider.name}')
  @PactFolder('pacts')
  class ProviderFromSystemPropTestClass {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @Consumer('${consumer.name}')
  @PactFolder('pacts')
  class ConsumerFromSystemPropTestClass {
    @TestTarget
    Target target
  }

  def 'PactRunner throws an exception if there is no @Provider annotation on the test class'() {
    when:
    new PactRunner(PactRunnerSpec).run(new RunNotifier())

    then:
    InitializationError e = thrown()
    e.causes*.message ==
      ['Provider name should be specified by using Provider annotation']
  }

  def 'PactRunner throws an exception if there is no pact source'() {
    when:
    new PactRunner(NoSourceTestClass).run(new RunNotifier())

    then:
    InitializationError e = thrown()
    e.causes*.message == ['Did not find any PactSource annotations. Exactly one pact source should be set']
  }

  def 'PactRunner throws an exception if the pact source throws an IO exception'() {
    when:
    new PactRunner(FailsTestClass).run(new RunNotifier())

    then:
    InitializationError e = thrown()
    e.causes.every { IOException.isAssignableFrom(it.class) }
  }

  def 'PactRunner throws an exception if there are no pacts to verify'() {
    when:
    new PactRunner(NoPactsTestClass).run(new RunNotifier())

    then:
    InitializationError e = thrown()
    e.causes*.message == ['Did not find any pact files for provider Bob']
  }

  def 'PactRunner only initializes once if run() is called multiple times'() {
    when:
    def runner = new PactRunner(TestClass)
    runner.run(new RunNotifier())
    def children1 = runner.children.clone()

    runner.run(new RunNotifier())
    def children2 = runner.children.clone()

    then:
    children1 == children2
  }

  def 'PactRunner does not throw an exception if there are no pacts to verify and @IgnoreNoPactsToVerify'() {
    when:
    new PactRunner(NoPactsIgnoredTestClass)

    then:
    notThrown(InitializationError)
  }

  @SuppressWarnings('LineLength')
  def 'PactRunner does not throw an exception if there is an IO error and @IgnoreNoPactsToVerify has ignoreIoErrors set'() {
    when:
    new PactRunner(DoesNotFailsTestClass)

    then:
    notThrown(InitializationError)
  }

  def 'PactRunner throws an exception if there is both a pact source and pact loader annotation'() {
    when:
    new PactRunner(BothPactSourceAndPactLoaderTestClass).run(new RunNotifier())

    then:
    InitializationError e = thrown()
    e.causes[0].message.startsWith('Exactly one pact source should be set, found 2: ')
  }

  def 'PactRunner handles a pact source with a pact loader that takes a class parameter'() {
    when:
    def runner = new PactRunner(PactLoaderWithConstructorParameterTestClass)
    runner.run(new RunNotifier())

    then:
    !runner.children.empty
  }

  def 'PactRunner handles a pact source with a pact loader that does not takes a class parameter'() {
    when:
    def runner = new PactRunner(PactLoaderWithDefaultConstructorClass)
    runner.run(new RunNotifier())

    then:
    !runner.children.empty
  }

  def 'PactRunner loads the pact loader class from the pact loader associated with the pact loader annotation'() {
    when:
    def runner = new PactRunner(TestClass)
    runner.run(new RunNotifier())

    then:
    !runner.children.empty
  }

  @Issue('#528')
  @RestoreSystemProperties
  def 'PactRunner supports getting the provider name from a system property or environment variable'() {
    given:
    System.setProperty('provider.name', 'myAwesomeService')

    when:
    def runner = new PactRunner(ProviderFromSystemPropTestClass)
    runner.run(new RunNotifier())

    then:
    !runner.children.empty
  }

  @Issue('#528')
  @RestoreSystemProperties
  def 'PactRunner supports getting the consumer name from a system property or environment variable'() {
    given:
    System.setProperty('consumer.name', 'anotherService')

    when:
    def runner = new PactRunner(ConsumerFromSystemPropTestClass)
    runner.run(new RunNotifier())

    then:
    !runner.children.empty
  }

  @Issue('#1692')
  def 'PactRunner supports V4 Pacts'() {
    when:
    new PactRunner(V4TestClass).run(new RunNotifier())

    then:
    notThrown(InitializationError)
  }

  @Provider('ExpectedName')
  static class ProviderWithName { }

  @Provider('${provider.name}')
  static class ProviderWithExpression { }

  @Provider
  static class ProviderWithNoName { }

  @Issue('#1630')
  @RestoreSystemProperties
  def 'lookup provider info - #clazz.simpleName'() {
    given:
    System.setProperty('provider.name', 'ExpectedName')
    System.setProperty('pact.provider.name', 'ExpectedName')

    expect:
    new PactRunner(clazz).lookupProviderInfo().second == 'ExpectedName'

    where:
    clazz << [ ProviderWithName, ProviderWithExpression, ProviderWithNoName ]
  }

  @Consumer('ExpectedName')
  static class ConsumerWithName { }

  @Consumer('${consumer.name}')
  static class ConsumerWithExpression { }

  @Consumer
  static class ConsumerWithNoName { }

  @Issue('#1630')
  @RestoreSystemProperties
  def 'lookup consumer info - #clazz.simpleName'() {
    given:
    System.setProperty('consumer.name', 'ExpectedName')
    System.setProperty('pact.consumer.name', 'ExpectedName')

    expect:
    new PactRunner(clazz).lookupConsumerInfo().second == name

    where:

    clazz                  | name
    ConsumerWithName       | 'ExpectedName'
    ConsumerWithExpression | 'ExpectedName'
    ConsumerWithNoName     | ''
  }
}
