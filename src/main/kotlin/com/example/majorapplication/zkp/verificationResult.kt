package com.example.majorapplication.zkp

import com.example.majorapplication.messages.StreamSessionResponse
import com.example.majorapplication.messages.VerificationResultType

enum class ErrorType {
    JAVA_TO_RUST_CONVERSION_ERROR,
    EMPTY_VECTORS_ERROR,
    DESERIALIZATION_ERROR,
    PROOF_ERROR;

    // Add more error types here in the future without worrying about codes
}

// Define the verification status as a sealed class hierarchy
sealed class VerificationResult {
    // Success case
    data object Present : VerificationResult()

    // Expected failure case
    data object Absent : VerificationResult()

    // Error case that contains the specific error type
    data class Error(val errorType: ErrorType) : VerificationResult()

    companion object {
        // Convert from the integer code coming from JNI
        fun fromCode(code: Int): VerificationResult {
            return when (code) {
                0 -> Present
                1 -> Absent
                10 -> Error(ErrorType.JAVA_TO_RUST_CONVERSION_ERROR)
                20 -> Error(ErrorType.EMPTY_VECTORS_ERROR)
                30 -> Error(ErrorType.DESERIALIZATION_ERROR)
                40 -> Error(ErrorType.PROOF_ERROR)
                else -> throw IllegalArgumentException("Unknown status code: $code")
            }
        }

        fun toCode(verificationResult: VerificationResult): Int {
            return when(verificationResult){
                Present -> 0
                Absent -> 1
                Error(ErrorType.JAVA_TO_RUST_CONVERSION_ERROR) -> 10
                Error(ErrorType.EMPTY_VECTORS_ERROR) -> 20
                Error(ErrorType.DESERIALIZATION_ERROR) -> 30
                Error(ErrorType.PROOF_ERROR)-> 40
                is Error -> -1
            }
        }

        fun VerificationResult.toProto(): StreamSessionResponse {
            return when (this) {
                VerificationResult.Present -> StreamSessionResponse.newBuilder()
                    .setResult(VerificationResultType.PRESENT)
                    .build()

                VerificationResult.Absent -> StreamSessionResponse.newBuilder()
                    .setResult(VerificationResultType.ABSENT)
                    .build()

                is VerificationResult.Error -> StreamSessionResponse.newBuilder()
                    .setResult(VerificationResultType.ERROR)
                    .setError(
                        when(this.errorType){
                            ErrorType.PROOF_ERROR -> com.example.majorapplication.messages.ErrorType.PROOF_ERROR
                            ErrorType.DESERIALIZATION_ERROR -> com.example.majorapplication.messages.ErrorType.DESERIALIZATION_ERROR
                            ErrorType.EMPTY_VECTORS_ERROR -> com.example.majorapplication.messages.ErrorType.EMPTY_VECTORS_ERROR
                            ErrorType.JAVA_TO_RUST_CONVERSION_ERROR -> com.example.majorapplication.messages.ErrorType.JAVA_TO_RUST_CONVERSION_ERROR
                            else -> com.example.majorapplication.messages.ErrorType.UNRECOGNIZED
                        }
                    )
                    .build()
            }
        }

        // Conversion from protobuf message back to Kotlin VerificationResult
        fun StreamSessionResponse.toKotlin(): VerificationResult {
            return when (this.result) {
                VerificationResultType.PRESENT -> VerificationResult.Present
                VerificationResultType.ABSENT -> VerificationResult.Absent
                VerificationResultType.ERROR -> VerificationResult.Error(
                    when (this.error) {
                        com.example.majorapplication.messages.ErrorType.JAVA_TO_RUST_CONVERSION_ERROR -> ErrorType.JAVA_TO_RUST_CONVERSION_ERROR
                        com.example.majorapplication.messages.ErrorType.EMPTY_VECTORS_ERROR -> ErrorType.EMPTY_VECTORS_ERROR
                        com.example.majorapplication.messages.ErrorType.DESERIALIZATION_ERROR -> ErrorType.DESERIALIZATION_ERROR
                        com.example.majorapplication.messages.ErrorType.PROOF_ERROR -> ErrorType.PROOF_ERROR
                        else -> throw IllegalArgumentException("Unknown error type")
                    }
                )
                else -> throw IllegalArgumentException("Unknown verification result type")
            }
        }


    }
}
