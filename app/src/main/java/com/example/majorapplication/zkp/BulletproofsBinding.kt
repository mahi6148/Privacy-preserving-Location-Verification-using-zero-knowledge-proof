package com.example.majorapplication.zkp
import android.util.Log

class BulletproofsBinding {
    companion object {
        init {
            try {
                System.loadLibrary("zkp_proximity").apply { Log.d("successLib","library loaded successfully from ${this.javaClass}") }

            } catch (e: UnsatisfiedLinkError) {
                Log.e("BulletproofsBinding", "Error loading library: ${e.message}")
                throw e
            }
        }

        @JvmStatic
        private external fun startProof(x1: Double, y1: Double, x2: Double, y2: Double): ProofData?

        fun generateProofForCoordinates(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): ProofData? {
            return startProof(lat1, lon1, lat2, lon2)
        }

    }
}
