package info.novatec.hackingtf.azure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import info.novatec.hackingtf.jsonPath

class GenericAzureConfigurationBlock(
    private val name: String,
    private val attributes: Map<String, Any?>,
    private val schemas: Map<String, Any?>,
    private val type: String? = null
) {
    private val attributeSchemas = schemas.jsonPath<Map<String, Any?>?>("$.attributes")
    private val blockSchemas = schemas.jsonPath<Map<String, Any?>?>("$.block_types")

    fun resourceCode(): String =
        """
        resource "$type" "$name" {
          ${attributesCode(attributes.keys.sorted())}
          
          ${blocksCode(attributes.keys.sorted())}
        }
    """.trimIndent()

    private fun blockCode() =
        """
        $name {
          ${attributesCode(attributes.keys.sorted())}
                    
          ${blocksCode(attributes.keys.sorted())}
        }
        """.trimIndent()

    private fun attributesCode(attributeNames: Iterable<String>): String =
        attributeNames
            .filter { attributeSchemas?.get(it) != null }
            .filterNot { attributeSchemas?.get(it)!!.jsonPath("$.computed") ?: false }
            .joinToString("\n", transform = ::attributeCode)

    private fun blocksCode(attributeNames: List<String>): String =
        attributeNames
            .filter { blockSchemas?.get(it) != null }
            .joinToString("\n") { blockName ->
                val blockSchema = (blockSchemas?.get(blockName) ?: error("No block schema for $blockName found"))
                    .jsonPath<Map<String, Any?>>("$.block")
                when (val blockAttribute = attributes[blockName]) {

                    // In case we have a list, there are multiple blocks of the same type. We generate them all.
                    is List<*> -> {
                        blockAttribute
                            .map { attributes ->
                                GenericAzureConfigurationBlock(
                                    blockName,
                                    attributes as Map<String, Any?>,
                                    blockSchema
                                ).blockCode()
                            }.joinToString("\n")
                    }

                    // In case we have a map, there is only one instance and we generate it.
                    is Map<*, *> ->
                        GenericAzureConfigurationBlock(
                            blockName,
                            blockAttribute as Map<String, Any?>,
                            blockSchema as Map<String, Any?>
                        ).blockCode()

                    else -> throw IllegalArgumentException("Unknown block type for $blockName")
                }
            }

    private fun attributeCode(attribute: String): String =
        "$attribute = ${jacksonObjectMapper().writeValueAsString(attributes[attribute])}"
}
