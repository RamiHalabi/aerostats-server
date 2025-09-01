package plugin

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext

class UserIdContext(val userId: String) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<UserIdContext>
    override val key: CoroutineContext.Key<*> = Key
}

/**
 * Retrieves the user ID from the coroutine context.
 * This should be called from a coroutine that has UserIdContext installed.
 * @throws IllegalStateException if UserId is not found in the context.
 */
suspend fun getUserIdFromContext(): String {
    return currentCoroutineContext()[UserIdContext.Key]?.userId
        ?: throw IllegalStateException("UserId not found in CoroutineContext.")
}