/**
 * Utilities for testing frontend
 */

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.requestStatusContext
import kotlinx.js.timers.setTimeout

import org.w3c.fetch.Response
import react.FC
import react.PropsWithChildren
import react.useState

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.Promise

val wrapper: FC<PropsWithChildren> = FC {
    val (_, setMockState) = useState<Response?>(null)
    val (_, setRedirectToFallbackView) = useState(false)
    val (_, setLoadingCounter) = useState(0)
    requestStatusContext.Provider {
        value = RequestStatusContext(setMockState, setRedirectToFallbackView, setLoadingCounter)
        +it.children
    }
}

/**
 * Mocks a successful response with serialized value of [value] and returns the same [response]
 *
 * @param response a response object from MSW library
 * @param value value to be added into response body
 * @return response object with configuration applied
 */
inline fun <reified T> mockMswResponse(response: dynamic, value: T): dynamic {
    response.status = 200
    response.headers.set("Content-Type", "application/json")
    response.body = Json.encodeToString(value)
    return response
}

fun wait(millis: Int) = Promise { resolve, _ ->
    setTimeout({ resolve(Unit) }, 200)
}
