package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.Utils.randomPort
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser
import java.io.File
import java.security.KeyStore

/**
 * The type of provider (synchronous or asynchronous)
 */
enum class ProviderType {
  /**
   * Synchronous provider (HTTP)
   */
  SYNCH,
  /**
   * Asynchronous provider (Messages)
   */
  ASYNCH,
  /**
   * Synchronous message provider
   */
  SYNCH_MESSAGE,
  /**
   * Unspecified, will default to synchronous
   */
  UNSPECIFIED
}

data class ProviderInfo @JvmOverloads constructor(
        val providerName: String = "",
        val hostInterface: String = "",
        val port: String = "",
        val pactVersion: PactSpecVersion? = null,
        val providerType: ProviderType? = null,
        val https: Boolean = false,
        @Deprecated("This has been replaced with the @MockServer annotation")
        val mockServerImplementation: MockServerImplementation = MockServerImplementation.Default,
        val keyStorePath: String = "",
        val keyStoreAlias: String = "",
        val keyStorePassword: String = "",
        val privateKeyPassword: String = "",
) {
  fun mockServerConfig() = if (https) {
    if(keyStorePath.isEmpty()) httpsProviderConfig() else httpsKeyStoreProviderConfig()
  } else {
    MockProviderConfig.httpConfig(
            hostInterface.ifEmpty { MockProviderConfig.LOCALHOST },
            if (port.isEmpty()) 0 else port.toInt(),
            pactVersion ?: PactSpecVersion.V4,
            mockServerImplementation
    )
  }

  private fun httpsProviderConfig() : MockHttpsProviderConfig {
    return MockHttpsProviderConfig.httpsConfig(
      hostInterface.ifEmpty { MockProviderConfig.LOCALHOST },
            if (port.isEmpty()) 0 else port.toInt(),
            pactVersion ?: PactSpecVersion.V3,
            mockServerImplementation)
  }

  private fun httpsKeyStoreProviderConfig() : MockHttpsProviderConfig {
    val loadedKeyStore = KeyStore.getInstance(File(keyStorePath), keyStorePassword.toCharArray())
    return MockHttpsProviderConfig(if (hostInterface.isEmpty()) MockProviderConfig.LOCALHOST else hostInterface,
            if (port.isEmpty() || port == "0") randomPort() else port.toInt(),
            pactVersion ?: PactSpecVersion.V3,
            loadedKeyStore,
            keyStoreAlias,
            keyStorePassword,
            privateKeyPassword,
            MockServerImplementation.KTorServer)
  }

  fun merge(other: ProviderInfo): ProviderInfo {
    return copy(providerName = providerName.ifEmpty { other.providerName },
            hostInterface = hostInterface.ifEmpty { other.hostInterface },
            port = port.ifEmpty { other.port },
            pactVersion = pactVersion ?: other.pactVersion,
            providerType = providerType ?: other.providerType,
            https = https || other.https,
            mockServerImplementation = mockServerImplementation.merge(other.mockServerImplementation),
            keyStorePath = keyStorePath.ifEmpty { other.keyStorePath },
            keyStoreAlias = keyStoreAlias.ifEmpty { other.keyStoreAlias },
            keyStorePassword = keyStorePassword.ifEmpty { other.keyStorePassword },
            privateKeyPassword = privateKeyPassword.ifEmpty { other.privateKeyPassword }
    )
  }

  fun withMockServerConfig(mockServerConfig: MockProviderConfig?): ProviderInfo {
    return if (mockServerConfig != null) {
      this.copy(hostInterface = mockServerConfig.hostname,
        port = if (mockServerConfig.port > 0) mockServerConfig.port.toString() else "",
        pactVersion = mockServerConfig.pactVersion.or(pactVersion), https = mockServerConfig.scheme == "https",
        mockServerImplementation = mockServerConfig.mockServerImplementation.merge(mockServerImplementation))
    } else {
      this
    }
  }

  companion object {
    fun fromAnnotation(annotation: PactTestFor): ProviderInfo {
      val providerName = ExpressionParser().parseExpression(annotation.providerName, DataType.STRING)?.toString()
        ?: annotation.providerName
      val pactVersion = when (annotation.pactVersion) {
        PactSpecVersion.UNSPECIFIED -> null
        else -> annotation.pactVersion
      }
      val providerType = when (annotation.providerType) {
        ProviderType.UNSPECIFIED -> null
        else -> annotation.providerType
      }
      val port = ExpressionParser().parseExpression(annotation.port, DataType.STRING)?.toString() ?: annotation.port
      return ProviderInfo(providerName, annotation.hostInterface, port, pactVersion, providerType,
        annotation.https, annotation.mockServerImplementation, annotation.keyStorePath, annotation.keyStoreAlias,
        annotation.keyStorePassword, annotation.privateKeyPassword)
    }
  }
}
