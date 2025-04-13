package com.example.majorapplication.zkp

class Verifier {
    companion object {
        init {
            try {
                System.loadLibrary("zkp_proximity")
                println("done loading library")
            }catch (e:Exception){
                println("error loading zkp_proximity")
                e.printStackTrace()

            }

        }

        @JvmStatic
        external fun startVerification(proofData: ProofData): Int

        fun verify(proofData: ProofData): VerificationResult {
            val statusCode = startVerification(proofData)
            return VerificationResult.fromCode(statusCode)
        }

    }


}