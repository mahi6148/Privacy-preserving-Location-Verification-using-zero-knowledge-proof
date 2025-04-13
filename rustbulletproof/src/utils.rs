use base64::Engine;
use base64::engine::general_purpose;
use bulletproofs::{ProofError, RangeProof};
use curve25519_dalek_ng::ristretto::CompressedRistretto;
use jni::JNIEnv;
use jni::objects::JObject;
use jni::sys::jint;
use crate::ERROR_JAVA_TO_RUST_CONVERSION;

pub fn serializer(serializable:Result<(RangeProof, CompressedRistretto), ProofError>) -> (Vec<u8>, Vec<u8>) {
    let obj = match serializable {
        // Ok(o) => (o.0.to_bytes(),serialize_compressed_ristretto(&o.1).unwrap()),
        Ok(o) => (serialize_rangeproof(&o.0).unwrap(),serialize_compressed_ristretto(&o.1).unwrap()),
        Err(..) => (Vec::new(), Vec::new())
    };

    obj
}

fn serialize_rangeproof(rp:&RangeProof)->Result<Vec<u8>, bincode::Error>{
    bincode::serialize(&rp)
}

fn deserialize_rangeproof(rangeproof_bytes:&Vec<u8>)->Result<RangeProof, bincode::Error>{
    bincode::deserialize(&rangeproof_bytes)
}

fn serialize_compressed_ristretto(compressed_point: &CompressedRistretto) -> Result<Vec<u8>, bincode::Error> { // Or serde_json::Error, serde_cbor::Error
    bincode::serialize(&compressed_point) // Or serde_json::to_vec, serde_cbor::to_vec
}

fn deserialize_compressed_ristretto(bytes: &Vec<u8>) -> Result<CompressedRistretto, bincode::Error> { // Or serde_json::Error, serde_cbor::Error
    bincode::deserialize(&bytes) // Or serde_json::from_slice, serde_cbor::from_slice
}

pub fn deserializer(rp_byte_array:Vec<u8>,cr_byte_array:Vec<u8>) -> Result<(RangeProof,CompressedRistretto),ProofError> {
    let rp = deserialize_rangeproof(&rp_byte_array).unwrap();
    let cr = deserialize_compressed_ristretto(&cr_byte_array).unwrap();
    Ok((rp,cr))

}

pub fn get_byte_array_field(env: &JNIEnv, obj: JObject, field_name: &str) -> Result<Vec<u8>, jint> {
    // Get the field as a JValue
    let array = env.get_field(obj, field_name, "[B")
        .map_err(|_| ERROR_JAVA_TO_RUST_CONVERSION)?;

    // Convert to JObject
    let array_obj = array.l()
        .map_err(|_| ERROR_JAVA_TO_RUST_CONVERSION)?;

    // Convert to Rust Vec<u8>
    env.convert_byte_array(array_obj.into_inner())
        .map_err(|_| ERROR_JAVA_TO_RUST_CONVERSION)
}

pub fn print_to_system_out(env: &JNIEnv, message: &str) {
    // Get the System.out PrintStream
    let system_class = env
        .find_class("java/lang/System")
        .expect("Couldn't find System class");

    let out_field = env
        .get_static_field(system_class, "out", "Ljava/io/PrintStream;")
        .expect("Couldn't get System.out field");

    // Create a Java string from our message
    let j_string = env
        .new_string(message)
        .expect("Couldn't create Java string!");

    // Call System.out.println
    env.call_method(
        out_field.l().unwrap(),
        "println",
        "(Ljava/lang/String;)V",
        &[j_string.into()]
    ).expect("Couldn't call println method");
}

// Helper function to log to Android logcat
pub fn log_to_android(env: &JNIEnv, tag: &str, message: &str) {
    let log_class = env
        .find_class("android/util/Log")
        .expect("Couldn't find Log class");

    let tag_jstring = env
        .new_string(tag)
        .expect("Couldn't create tag string");

    let msg_jstring = env
        .new_string(message)
        .expect("Couldn't create message string");

    // Call Log.d() 
    env.call_static_method(
        log_class,
        "d",
        "(Ljava/lang/String;Ljava/lang/String;)I",
        &[tag_jstring.into(), msg_jstring.into()]
    ).expect("Couldn't call Log.d method");
}

pub fn range_proof_to_string(proof: &RangeProof) -> String {
    // Serialize the RangeProof to bytes
    let proof_bytes = proof.to_bytes();

    // Encode the bytes as base64
    general_purpose::STANDARD.encode(&proof_bytes)
}

/// Converts a base64-encoded string back to a RangeProof
pub fn string_to_range_proof(encoded: &str) -> Result<RangeProof, ProofError> {
    // Decode the base64 string to bytes
    let proof_bytes = general_purpose::STANDARD.decode(encoded)
        .map_err(|_| ProofError::FormatError)?;

    // Convert bytes back to RangeProof
    RangeProof::from_bytes(&proof_bytes)
}

/// Converts a CompressedRistretto point to a base64-encoded string
pub fn compressed_ristretto_to_string(point: &CompressedRistretto) -> String {
    // CompressedRistretto is already a 32-byte array
    let point_bytes = point.to_bytes();

    // Encode the bytes as base64
    general_purpose::STANDARD.encode(&point_bytes)
}
