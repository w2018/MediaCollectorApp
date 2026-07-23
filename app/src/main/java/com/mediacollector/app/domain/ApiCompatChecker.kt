package com.mediacollector.app.domain

import com.mediacollector.app.data.remote.api.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API 兼容性检测器
 *
 * 对每个端点发送请求，验证响应格式，给出兼容性评级。
 */
@Singleton
class ApiCompatChecker @Inject constructor(
    private val mediaApi: MediaApi,
    private val authApi: AuthApi,
    private val systemApi: SystemApi,
    private val userApi: UserApi
) {
    data class ApiCheckResult(
        val overallStatus: OverallStatus,
        val checks: List<EndpointCheck>,
        val serverName: String = "",
        val apiVersion: String = "",
        val mediaCount: Int = 0
    )

    data class EndpointCheck(
        val name: String,
        val status: CheckStatus,
        val statusCode: Int = 0,
        val error: String = ""
    )

    enum class OverallStatus {
        FULLY_COMPATIBLE,   // 完全兼容
        PARTIALLY_COMPATIBLE, // 基本兼容
        INCOMPATIBLE         // 不兼容
    }

    enum class CheckStatus {
        OK,     // ✅ 正常
        WARN,   // ⚠️ 部分异常
        FAIL    // ❌ 失败
    }

    /** 执行完整检测 */
    suspend fun check(): ApiCheckResult {
        val checks = mutableListOf<EndpointCheck>()

        // 1. 检测 /api/v1/stats
        checks.add(runCheck("GET /api/v1/stats") { mediaApi.getStats() })

        // 2. 检测 /api/v1/media
        checks.add(runCheck("GET /api/v1/media") { mediaApi.getMediaList(pageSize = 1) })

        // 3. 检测 /api/v1/collections
        checks.add(runCheck("GET /api/v1/collections") { mediaApi.getCollections() })

        // 4. 检测 /api/v1/tags
        checks.add(runCheck("GET /api/v1/tags") { mediaApi.getTags() })

        // 5. 检测 /api/v1/search
        checks.add(runCheck("GET /api/v1/search") { mediaApi.search("test") })

        // 6. 检测 /api/v1/system/check
        var serverName = ""
        var apiVersion = ""
        var mediaCount = 0
        try {
            val sysResponse = systemApi.check()
            if (sysResponse.success && sysResponse.data != null) {
                checks.add(EndpointCheck("GET /api/v1/system/check", CheckStatus.OK, 200))
                serverName = sysResponse.data.serverName
                apiVersion = sysResponse.data.apiVersion
                mediaCount = sysResponse.data.mediaCount
            } else {
                checks.add(EndpointCheck("GET /api/v1/system/check", CheckStatus.WARN))
            }
        } catch (e: Exception) {
            checks.add(EndpointCheck("GET /api/v1/system/check", CheckStatus.FAIL, error = e.message ?: "未知错误"))
        }

        // 7. 检测 /api/v1/auth/login（仅检查端点可达性）
        val loginCheck = runCatching {
            authApi.login(com.mediacollector.app.data.remote.dto.LoginRequest("test", "test123"))
        }.fold(
            onSuccess = { EndpointCheck("POST /api/v1/auth/login", CheckStatus.OK, 200) },
            onFailure = { e ->
                // 400 表示端点可达（参数校验通过），401 表示用户名/密码错，都说明端点正常
                val httpCode = (e as? retrofit2.HttpException)?.code() ?: 0
                if (httpCode in listOf(400, 401)) {
                    EndpointCheck("POST /api/v1/auth/login", CheckStatus.OK, httpCode)
                } else {
                    EndpointCheck("POST /api/v1/auth/login", CheckStatus.FAIL, error = e.message ?: "连接失败")
                }
            }
        )
        checks.add(loginCheck)

        // 计算总体状态
        val okCount = checks.count { it.status == CheckStatus.OK }
        val failCount = checks.count { it.status == CheckStatus.FAIL }
        val overallStatus = when {
            failCount == 0 && okCount == checks.size -> OverallStatus.FULLY_COMPATIBLE
            failCount == checks.size -> OverallStatus.INCOMPATIBLE
            else -> OverallStatus.PARTIALLY_COMPATIBLE
        }

        return ApiCheckResult(overallStatus, checks, serverName, apiVersion, mediaCount)
    }

    private suspend fun runCheck(name: String, block: suspend () -> Any): EndpointCheck {
        return try {
            block()
            EndpointCheck(name, CheckStatus.OK, 200)
        } catch (e: retrofit2.HttpException) {
            // HTTP 错误也算端点可达（只是请求本身有问题）
            EndpointCheck(name, CheckStatus.WARN, e.code())
        } catch (e: Exception) {
            EndpointCheck(name, CheckStatus.FAIL, error = e.message ?: "连接失败")
        }
    }
}
