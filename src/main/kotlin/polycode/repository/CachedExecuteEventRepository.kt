package polycode.repository

import polycode.model.result.ExecuteEvent

interface CachedExecuteEventRepository {
    fun insertAll(events: List<ExecuteEvent>)
}
