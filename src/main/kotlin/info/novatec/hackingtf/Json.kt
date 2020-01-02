package info.novatec.hackingtf

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option

inline fun <reified T> Any.jsonPath(path: String): T = JsonPath
    .using(Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL))
    .parse(this)
    .read(path)
