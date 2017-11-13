import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result

fun main(args: Array<String>) {

    val response = getClientCredential("http://localhost:5000/connect/token", "kotlin_oauth", "client_password", listOf("api1.read", "api1.write"))
            .bind({ tokenResponse -> callApi("http://localhost:5000/api", tokenResponse.tokenType, tokenResponse.token) })

    when (response) {
        is Response.Success -> {
            println(response.value)
        }
        is Response.Failure -> {
            println(response.error)
        }
    }
}

fun getClientCredential(tokenEndpoint: String, clientId: String, clientSecret: String, scopes: List<String>): Response<TokenResponse, String> {
    val (request, response, result) = tokenEndpoint.httpPost(listOf(
            "grant_type" to "client_credentials",
            "scope" to scopes.joinToString(" ")))
            .authenticate(clientId, clientSecret)
            .responseString()

    return when (result) {
        is Result.Success -> {
            val parser = Parser()
            val json = parser.parse(StringBuilder(result.value)) as JsonObject

            Response.of(TokenResponse(
                    json["access_token"].toString(),
                    json["expires_in"].toString().toInt(),
                    json["token_type"].toString()))
        }
        is Result.Failure -> {
            Response.error("Token request failed")
        }
    }
}

fun callApi(apiEndpoint: String, tokenType: String, token: String): Response<String, String> {
    val (request, response, result) = apiEndpoint.httpGet().header(Pair("Authorization", "$tokenType $token")).responseString()

    return when (result) {
        is Result.Success -> {
            Response.of(result.value)
        }
        is Result.Failure -> {
            return Response.error("API request failed")
        }
    }
}

class TokenResponse(val token: String, val expiresIn: Int, val tokenType: String)

sealed class Response<out L, R> {
    class Success<out L, R>(val value: L) : Response<L, R>()
    class Failure<out L, R>(val error: R) : Response<L, R>()

    fun <X> bind(success: (L) -> (Response<X, R>)): Response<X, R> {
        return when (this) {
            is Response.Success<L, R> -> success(this.value)
            is Response.Failure<L, R> -> Failure(this.error)
        }
    }

    companion object {
        fun <L, R> of(response: L) = Success<L, R>(response)
        fun <L, R> error(error: R) = Failure<L, R>(error)
    }
}