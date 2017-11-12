import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result

fun main(args: Array<String>) {
    val response: TokenResponse = getClientCredential("http://localhost:5000/connect/token", "kotlin_oauth", "client_password")

    println(response.token)
    println(response.expiresIn)
    println(response.tokenType)

    val apiResponse = callApi("http://localhost:5000/api", response.tokenType, response.token)

    println(apiResponse)
}

fun getClientCredential(tokenEndpoint: String, clientId: String, clientSecret: String): TokenResponse {
    val (request, response, result) = tokenEndpoint.httpPost(listOf(
            "grant_type" to "client_credentials"))
            .authenticate(clientId, clientSecret)
            .responseString()

    when (result) {
        is Result.Success -> {
            val parser = Parser()
            val json = parser.parse(StringBuilder(result.value)) as JsonObject

            return TokenResponse(
                    json["access_token"].toString(),
                    json["expires_in"].toString().toInt(),
                    json["token_type"].toString())
        }
        is Result.Failure -> {
            // handle error
            throw SecurityException()
        }
    }
}

fun callApi(apiEndpoint: String, tokenType: String, token: String): String {
    val (request, response, result) = apiEndpoint.httpGet().header(Pair("Authorization", "$tokenType $token")).responseString()

    return when (result) {
        is Result.Success -> {
            result.value
        }
        is Result.Failure -> {
            "error!"
        }
    }
}

class TokenResponse(val token: String, val expiresIn: Int, val tokenType: String)