// use bulletproofs::{BulletproofGens, PedersenGens};
// 
// mod distance;
// mod prover;
// mod utils;
// mod build;
// 
// #[repr(C)]
// pub struct ProofData{
//     proof_and_ristretto: (Vec<u8>, Vec<u8>),
//     bulletproof_gens: BulletproofGens,
//     pedersen_gens: PedersenGens,
//     
// }
// 
// #[no_mangle]
// pub extern "C" fn start_proof(x1: f64, y1: f64, x2: f64, y2: f64)-> ProofData{
//     let c1 = geo_types::Coord { x: x1, y: y1 };
//     let c2 = geo_types::Coord { x: x2, y: y2 };
//     let mut distance:u64 = 0;
//     let res = distance::distance_from_coords(&c1, &c2);
//     match res {
//         Ok(d) => distance = (d * 1000.0) as u64,
//         Err(e) => println!("error: {}", e)
//     };
// 
//     let range_proof_res_obj = prover::fun_prover(distance);
//     
//     let pc_gens = range_proof_res_obj.0;
//     let bp_gens = range_proof_res_obj.1;
// 
//     let exportobj = utils::serializer(&range_proof_res_obj.2);
//     
//     ProofData{
//         proof_and_ristretto:exportobj,
//         bulletproof_gens: bp_gens,
//         pedersen_gens: pc_gens,
//         
//     }
//     
//     
// }
// 
// 


// // lib.rs
// 
// use std::mem;
// mod distance;
// mod prover;
// 
// #[repr(C)]
// pub struct ProofData {
//     proof_bytes: *const u8,
//     proof_len: usize,
//     ristretto_bytes: *const u8,
//     ristretto_len: usize,
//     bp_gens_capacity: usize,
//     bp_gens_party_capacity: usize,
//     // We'll serialize these into bytes for FFI
//     pedersen_bytes: [u8; 64], // 2 compressed points, 32 bytes each
// }
// 
// #[no_mangle]
// pub extern "C" fn start_proof(x1: f64, y1: f64, x2: f64, y2: f64) -> ProofData {
//     let c1 = geo_types::Coord { x: x1, y: y1 };
//     let c2 = geo_types::Coord { x: x2, y: y2 };
//     let mut distance: u64 = 0;
// 
//     if let Ok(d) = distance::distance_from_coords(&c1, &c2) {
//         distance = (d * 1000.0) as u64;
//     }
// 
//     let range_proof_res_obj = prover::fun_prover(distance);
//     let (pc_gens, bp_gens, proof_result) = range_proof_res_obj;
// 
//     match proof_result {
//         Ok((proof, ristretto)) => {
//             let proof_bytes = proof.to_bytes();
//             let ristretto_bytes = bincode::serialize(&ristretto).unwrap();
// 
//             // Convert Pedersen generators to bytes
//             let mut pedersen_bytes = [0u8; 64];
//             pedersen_bytes[..32].copy_from_slice(&pc_gens.B.compress().to_bytes());
//             pedersen_bytes[32..].copy_from_slice(&pc_gens.B_blinding.compress().to_bytes());
// 
//             let proof_ptr = proof_bytes.as_ptr();
//             let proof_len = proof_bytes.len();
//             let ristretto_ptr = ristretto_bytes.as_ptr();
//             let ristretto_len = ristretto_bytes.len();
// 
//             // Prevent deallocation
//             mem::forget(proof_bytes);
//             mem::forget(ristretto_bytes);
// 
//             ProofData {
//                 proof_bytes: proof_ptr,
//                 proof_len,
//                 ristretto_bytes: ristretto_ptr,
//                 ristretto_len,
//                 bp_gens_capacity: bp_gens.gens_capacity,
//                 bp_gens_party_capacity: bp_gens.party_capacity,
//                 pedersen_bytes,
//             }
//         }
//         Err(_) => ProofData {
//             proof_bytes: std::ptr::null(),
//             proof_len: 0,
//             ristretto_bytes: std::ptr::null(),
//             ristretto_len: 0,
//             bp_gens_capacity: 0,
//             bp_gens_party_capacity: 0,
//             pedersen_bytes: [0u8; 64],
//         }
//     }
// }
// 
// #[no_mangle]
// pub extern "C" fn free_proof_data(data: ProofData) {
//     if !data.proof_bytes.is_null() {
//         unsafe {
//             Vec::from_raw_parts(
//                 data.proof_bytes as *mut u8,
//                 data.proof_len,
//                 data.proof_len
//             );
//         }
//     }
// 
//     if !data.ristretto_bytes.is_null() {
//         unsafe {
//             Vec::from_raw_parts(
//                 data.ristretto_bytes as *mut u8,
//                 data.ristretto_len,
//                 data.ristretto_len
//             );
//         }
//     }
// }

use jni::JNIEnv;
use jni::objects::{JClass, JObject};
use jni::sys::{jdouble, jint, jobject};
use crate::utils::get_byte_array_field;
use crate::verifier::fun_verifier;

mod distance;
mod prover;
mod utils;
mod verifier;

pub struct ProofData {
    proof_bytes: Vec<u8>,
    ristretto_bytes: Vec<u8>,
}

impl Default for ProofData {
    fn default() -> Self {
        ProofData {
            proof_bytes: Vec::new(),
            ristretto_bytes: Vec::new(),
        }
    }
}

const PRESENT: jint = 0;
const ABSENT: jint = 1;
const ERROR_JAVA_TO_RUST_CONVERSION: jint = 10;
const ERROR_EMPTY_VECTORS: jint = 20;
const ERROR_DESERIALIZATION: jint = 30;
const ERROR_PROOF: jint = 40;




#[no_mangle]
pub extern "system" fn Java_com_example_majorapplication_zkp_BulletproofsBinding_startProof(
    env: JNIEnv,
    _class: JClass,
    x1: jdouble,
    y1: jdouble,
    x2: jdouble,
    y2: jdouble,
) -> jobject {



    let c1 = geo_types::Coord { x: x1 as f64, y: y1 as f64 };
    let c2 = geo_types::Coord { x: x2 as f64, y: y2 as f64 };
    let mut distance: u64 = 0;


    if let Ok(d) = distance::distance_from_coords(&c1, &c2) {
        distance = (d * 1000.0) as u64;
        utils::log_to_android(&env,"Native Printing from proof class", &distance.to_string() );
    }

    let range_proof_res_obj = prover::fun_prover(distance);
    let  proof_result = range_proof_res_obj;
    
    let stringrp = utils::range_proof_to_string(&proof_result.clone().unwrap().0);
    let stringcr = utils::compressed_ristretto_to_string(&proof_result.clone().unwrap().1);

    utils::log_to_android(&env,"Native Printing from proof class", &stringrp );
    utils::log_to_android(&env,"Native Printing from proof class", &stringcr );

    let (proof_bytes,ristretto_bytes) = utils::serializer(proof_result);
    
    let proof_data = ProofData{
        proof_bytes,
        ristretto_bytes
    };

    // Create Java ProofData object
    let proof_class = env.find_class("com/example/majorapplication/zkp/ProofData")
        .expect("Failed to find ProofData class");

    let proof_obj = env.new_object(proof_class, "()V", &[])
        .expect("Failed to create ProofData object");

    // Convert Rust vectors to Java byte arrays
    let proof_array = env.byte_array_from_slice(&proof_data.proof_bytes)
        .expect("Failed to create proof byte array");
    let ristretto_array = env.byte_array_from_slice(&proof_data.ristretto_bytes)
        .expect("Failed to create ristretto byte array");

    // Set fields in Java object
    env.set_field(proof_obj, "proofBytes", "[B", JObject::from(proof_array).into())
        .expect("Failed to set proofBytes");
    env.set_field(proof_obj, "ristrettoBytes", "[B", JObject::from(ristretto_array).into())
        .expect("Failed to set ristrettoBytes");

    proof_obj.into_inner()
}

#[no_mangle]
pub extern "system" fn Java_com_example_majorapplication_zkp_Verifier_startVerification(
    env: JNIEnv,
    _class: JClass,
    proof_data_obj: JObject,
) -> jint {
    // Extract fields from Java ProofData object
    let proof_bytes =  match get_byte_array_field(&env, proof_data_obj, "proofBytes") {
        Ok(bytes) => bytes,
        Err(error_code) => return error_code,
    };

    let ristretto_bytes =  match get_byte_array_field(&env, proof_data_obj, "ristrettoBytes") {
        Ok(bytes) => bytes,
        Err(error_code) => return error_code,
    };

    // Create ProofData struct
    let proof_data = ProofData {
        proof_bytes,
        ristretto_bytes,
    };

    if proof_data.proof_bytes.is_empty() || proof_data.ristretto_bytes.is_empty() {
        return ERROR_EMPTY_VECTORS;
    }

    // Now you can proceed with the verification logic using proof_data
    // For example:
    // let verification_result = verifier::verify_proof(&proof_data);
    // return if verification_result { JNI_TRUE } else { JNI_FALSE };
    
    let deserializedinfo = utils::deserializer(proof_data.proof_bytes,proof_data.ristretto_bytes);
    
    let (range_proof,compressed_risotto) = match deserializedinfo { 
        Ok(rp_cp_tuple) => rp_cp_tuple,
        Err(_) => return ERROR_DESERIALIZATION,
    };
    
    let stringrp = utils::range_proof_to_string(&range_proof);
    let stringcr = utils::compressed_ristretto_to_string(&compressed_risotto);

    utils::print_to_system_out(&env, &stringrp);
    utils::print_to_system_out(&env, &stringcr);
    
    let verification_result = fun_verifier(&range_proof,&compressed_risotto);
    match verification_result { 
        Ok(_) => PRESENT,
        Err(_) => ABSENT,
    }
}
