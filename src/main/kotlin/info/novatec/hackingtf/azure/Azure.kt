package info.novatec.hackingtf.azure

fun azureIdToNamedResource(id: String, name: String): String =
    when {
        Regex("^/subscriptions/.*/resourceGroups/.*/providers/Microsoft.DBforMySQL/servers/.*$").matches(id) -> "azurerm_mysql_server.$name"
        Regex("^/subscriptions/.*/resourceGroups/.*/providers/Microsoft.ContainerService/managedClusters/.*$").matches(id) -> "azurerm_kubernetes_cluster.$name"
        Regex("^/subscriptions/.*/resourceGroups/.*/providers/Microsoft.Network/virtualNetworks/.*$").matches(id) -> "azurerm_virtual_network.$name"
        Regex("^/subscriptions/.*/resourceGroups/.*$").matches(id) -> "azurerm_resource_group.$name"
        else -> throw IllegalArgumentException("Unknown resource type $id")
    }
