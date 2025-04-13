package com.example.majorapplication.zkp


data class ProofData(val proofBytes: ByteArray = ByteArray(0), val ristrettoBytes: ByteArray = ByteArray(0)) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProofData

        if (!proofBytes.contentEquals(other.proofBytes)) return false
        if (!ristrettoBytes.contentEquals(other.ristrettoBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = proofBytes.contentHashCode()
        result = 31 * result + ristrettoBytes.contentHashCode()
        return result
    }
}