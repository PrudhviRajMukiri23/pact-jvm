package au.com.dius.pact.core.matchers.generators

import au.com.dius.pact.core.matchers.JsonContentMatcher
import au.com.dius.pact.core.matchers.MatchingContext
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.JsonContentTypeHandler
import au.com.dius.pact.core.model.generators.JsonQueryResult
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.support.json.JsonValue
import io.github.oshai.kotlinlogging.KLogging

object ArrayContainsJsonGenerator : KLogging(), Generator {
  override val type: String
    get() = "ArrayContains"

  override fun generate(context: MutableMap<String, Any>, exampleValue: Any?): Any? {
    return if (exampleValue is JsonValue.Array) {
      for ((index, example) in exampleValue.values.withIndex()) {
        val variant = findMatchingVariant(example, context)
        if (variant != null) {
          logger.debug { "Generating values for variant $variant and value $example" }
          val json = JsonQueryResult(example)
          for ((key, generator) in variant.third) {
            JsonContentTypeHandler.applyKey(json, key, generator, context)
          }
          logger.debug { "Generated value ${json.value}" }
          exampleValue[index] = json.jsonValue ?: JsonValue.Null
        }
      }
      exampleValue
    } else {
      logger.error { "ArrayContainsGenerator can only be applied to lists" }
      null
    }
  }

  override fun toMap(pactSpecVersion: PactSpecVersion) = emptyMap<String, Any>()

  private fun findMatchingVariant(
    example: JsonValue,
    context: Map<String, Any?>
  ): Triple<Int, MatchingRuleCategory, Map<String, Generator>>? {
    val variants = context["ArrayContainsVariants"] as List<Triple<Int, MatchingRuleCategory, Map<String, Generator>>>
    return variants.firstOrNull { (index, rules, _) ->
      logger.debug { "Comparing variant $index with value '$example'" }
      // TODO: need to get any plugin config here
      val matchingContext = MatchingContext(rules, true)
      val matches = JsonContentMatcher.compare(listOf("$"), example, example, matchingContext)
      logger.debug { "Comparing variant $index => $matches" }
      matches.flatMap { it.result }.isEmpty()
    }
  }
}
