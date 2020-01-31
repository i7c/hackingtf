package info.novatec.hackingtf.terraform

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import info.novatec.hackingtf.jsonPath

class GenericConfigurationBlock(
    private val name: String,
    private val attributes: Map<String, Any?>,
    private val schemas: Map<String, Any?>,
    private val type: String? = null
) {
    private val attributeSchemas = schemas.jsonPath<Map<String, Any?>?>("$.attributes")
    private val blockSchemas = schemas.jsonPath<Map<String, Any?>?>("$.block_types")

    fun resourceCode(): String = """
        resource "$type" "$name" {
          ${attributesCode(attributes.keys.sorted())}
          ${blocksCode(attributes.keys.sorted())}
        }
        """

    private fun blockCode() = """
        $name {
          ${attributesCode(attributes.keys.sorted())}
          ${blocksCode(attributes.keys.sorted())}
        }"""

    private fun attributesCode(attributeNames: Iterable<String>): String =
        attributeNames
            .filter { attributeSchemas?.get(it) != null }
            .filterNot { attributeSchemas?.get(it)!!.jsonPath("$.computed") ?: false }
            .joinToString("\n", transform = ::attributeCode)

    private fun blocksCode(attributeNames: List<String>): String =
        attributeNames
            .filter { blockSchemas?.get(it) != null }
            .joinToString("") { blockName ->
                val blockSchema = (blockSchemas?.get(blockName) ?: error("No block schema for $blockName found"))
                    .jsonPath<Map<String, Any?>>("$.block")
                when (val blockAttribute = attributes[blockName]) {

                    // In case we have a list, there are multiple blocks of the same type. We generate them all.
                    is List<*> -> {
                        blockAttribute
                            .map { attributes ->
                                GenericConfigurationBlock(
                                    blockName,
                                    attributes as Map<String, Any?>,
                                    blockSchema
                                ).blockCode()
                            }.joinToString("\n")
                    }

                    else -> "# Unknown attribute type for attribute $blockName"
                }
            }

    private fun attributeCode(attribute: String) = "$attribute = ${jacksonObjectMapper().writeValueAsString(attributes[attribute])}"
}
