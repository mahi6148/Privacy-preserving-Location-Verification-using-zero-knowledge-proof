use bulletproofs::{BulletproofGens, PedersenGens, ProofError, RangeProof};
use curve25519_dalek_ng::ristretto::CompressedRistretto;
use merlin::Transcript;

pub fn fun_verifier(range_proof: &RangeProof,compressed_ristretto: &CompressedRistretto)->Result<(),ProofError>{

    let mut prover_transcript = Transcript::new(b"privacy enhanced Geolocation based attadance tracker using ZKP");

    range_proof.verify_single(
        &BulletproofGens::new(64,1),
        &PedersenGens::default(),
        &mut prover_transcript,
        compressed_ristretto,
        8
    )
}