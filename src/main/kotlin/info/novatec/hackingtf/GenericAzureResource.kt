package info.novatec.hackingtf

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class GenericAzureResource(
    private val resourceChanges: Map<String, Any?>,
    private val schema: Map<String, Any?>
) {
    val attributeSchemas = schema.jsonPath<Map<String, Any?>>("$.block.attributes")
    val attributes = resourceChanges.jsonPath<Map<String, Any?>>("$.change.before")

    fun resourceCode(): String =
        """
        resource "${resourceChanges["type"]}" "${resourceChanges["name"]}" {
          ${attributesCode(attributes.keys.sorted())}
        }
    """.trimIndent()

    private fun attributesCode(attributeNames: Iterable<String>): String =
        attributeNames
            .filter { attributeSchemas[it] != null }
            .filterNot { attributeSchemas[it]!!.jsonPath("$.computed") ?: false }
            .joinToString("\n", transform = ::attributeCode)

    private fun attributeCode(attribute: String): String =
        "$attribute = ${jacksonObjectMapper().writeValueAsString(attributes[attribute])}"
}
