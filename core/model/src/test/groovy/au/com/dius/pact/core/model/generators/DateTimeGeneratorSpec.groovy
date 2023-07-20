package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.support.Json
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@SuppressWarnings('LineLength')
class DateTimeGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new DateTimeGenerator('yyyy-MM-dd\'T\'HH:mm:ssZ', null).generate([:], null) ==~
      /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[-+]\d+/
  }

  def 'Uses any defined expression to generate the datetime value'() {
    expect:
    new DateTimeGenerator('yyyy-MM-dd\'T\'HH:mm:ssZ', '+ 1 day @ + 1 hour')
      .generate([baseDateTime: base], null) == datetime

    where:
    base = OffsetDateTime.now()
    datetime = base.plusDays(1).plusHours(1).format('yyyy-MM-dd\'T\'HH:mm:ssZ')
  }

  def 'Uses json deserialization to work correctly with optional format fields'() {
    given:
    def map = [:]
    def json = Json.INSTANCE.toJson(map).asObject()
    def baseDateTime = LocalDateTime.now()
    def baseWithOffset = baseDateTime.atOffset(ZoneOffset.ofHours(11))

    expect:
    DateTimeGenerator.@Companion.fromJson(json).generate([baseDateTime: baseWithOffset], null) ==
      baseDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  }

  def 'supports timezones with zone IDs'() {
    expect:
    new DateTimeGenerator("yyyy-MM-dd'T'HH:mm:ssZ'['VV']'", null).generate([:], null) ==~
      /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[-+]\d+\[\w+(\/\w+)?]/
  }
}
