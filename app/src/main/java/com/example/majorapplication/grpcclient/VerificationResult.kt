package com.example.majorapplication.grpcclient

enum class ErrorType {
    JAVA_TO_RUST_CONVERSION_ERROR,
    EMPTY_VECTORS_ERROR,
    DESERIALIZATION_ERROR,
    PROOF_ERROR,
    STREAM_ERROR,
    STREAM_COMPLETED,
    START_STREAMING_DEFAULT,
    DUPLICATE_SESSION_NAME

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

        fun VerificationResult.toTextString():String{
            return when (this){
                Present -> "Present"
                Absent -> "Absent"
                is Error -> when(this.errorType){
                    ErrorType.JAVA_TO_RUST_CONVERSION_ERROR -> "JAVA_TO_RUST_CONVERSION_ERROR"
                    ErrorType.EMPTY_VECTORS_ERROR -> "EMPTY_VECTORS_ERROR"
                    ErrorType.DESERIALIZATION_ERROR -> "DESERIALIZATION_ERROR"
                    ErrorType.PROOF_ERROR -> "PROOF_ERROR"
                    ErrorType.STREAM_ERROR -> "STREAM_ERROR"
                    ErrorType.STREAM_COMPLETED -> "STREAM_COMPLETED"
                    ErrorType.START_STREAMING_DEFAULT -> "START_STREAMING_DEFAULT"
                    ErrorType.DUPLICATE_SESSION_NAME -> "DUPLICATE_SESSION_NAME"
                }
            }
        }

    }
}
