package br.com.zup.edu.controller

import br.com.zup.edu.CalculaFreteRequest
import br.com.zup.edu.ErrorDetails
import br.com.zup.edu.FretesServiceGrpc
import com.google.protobuf.Any
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject

@Controller
class CalculadoraDeFreteController(@Inject val gRpcClient: FretesServiceGrpc.FretesServiceBlockingStub) {

    @Get("/api/fretes")
    fun calcula(@QueryValue cep: String): FreteResponse {

        val request = CalculaFreteRequest.newBuilder()
            .setCep(cep)
            .build()

       try {
           val response = gRpcClient.calculaFrete(request)

           return FreteResponse(
               cep = response.cep,
               valor = response.valor,
           )
       } catch (e: StatusRuntimeException) {

           val statusCode = e.status.code
           val description = e.status.description

           when (statusCode) {
               Status.Code.INVALID_ARGUMENT -> {
                   throw HttpStatusException(HttpStatus.BAD_REQUEST, e.message)
               }
               Status.Code.PERMISSION_DENIED -> {

                   val statusProto =
                       StatusProto.fromThrowable(e) ?: throw HttpStatusException(HttpStatus.FORBIDDEN, description)

                   val anyDetails: Any = statusProto.detailsList.get(0)//O Any utilizado aqui, deve ser do Protobuf
                   val errorDetails = anyDetails.unpack(ErrorDetails::class.java)

                   throw HttpStatusException(HttpStatus.FORBIDDEN, "${errorDetails.code}: ${errorDetails.message}")
               }
               Status.Code.NOT_FOUND -> {
                   throw HttpStatusException(HttpStatus.NOT_FOUND, e.message)
               }
               else -> throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
           }

       }

    }
}

data class FreteResponse(val cep: String, val valor: Double){

}
