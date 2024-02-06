use bitcoin::key::Secp256k1;
use bitcoin::secp256k1::SecretKey;
use bitcoin::{Address, Network, PrivateKey};
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use rand::rngs::OsRng;
use rand::RngCore;

use crate::jni_helper::{jobject_null, UnwrapOrThrow};
use crate::{java_str_var, new_java_string};

fn generate_secret() -> Result<SecretKey, bitcoin::secp256k1::Error> {
    let mut random = [0_u8; 32];
    OsRng.fill_bytes(&mut random);
    SecretKey::from_slice(&random)
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Bitcoin_generateKey(
    mut env: JNIEnv,
    _: JClass,
) -> jstring {
    let result: anyhow::Result<jstring> = try {
        let secret = generate_secret()?;
        let private_key = PrivateKey::new(secret, Network::Bitcoin);
        let wif = private_key.to_wif();
        env.new_string(&wif)?.into_raw()
    };
    result.unwrap_or_throw(&mut env, jobject_null())
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_pers_zhc_tools_jni_JNI_00024Bitcoin_privateKeyToAddress(
    mut env: JNIEnv,
    _: JClass,
    key: JString,
) -> jstring {
    let k1 = Secp256k1::new();
    let result: anyhow::Result<jstring> = try {
        java_str_var!(env, wif, key);
        let private_key = PrivateKey::from_wif(wif)?;
        let public_key = private_key.public_key(&k1);
        let address = Address::p2wpkh(&public_key, Network::Bitcoin)?;
        let address = address.to_string();
        new_java_string!(env, &address)
    };
    result.unwrap_or_throw(&mut env, jobject_null())
}
