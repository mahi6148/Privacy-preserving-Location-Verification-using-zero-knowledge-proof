package com.example.majorapplication.zkp

class BulletproofsBinding {
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
