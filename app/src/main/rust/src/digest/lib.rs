use std::io;
use std::io::{Read, Write};

use digest::generic_array::GenericArray;
use digest::typenum::Unsigned;
use digest::{Digest, ExtendableOutput, FixedOutput, HashMarker, OutputSizeUser, Update};

pub fn fixed_output_hash<H, R>(
    mut reader: R,
    iter_num: u64,
) -> io::Result<[u8; H::OutputSize::USIZE]>
where
    H: Digest + FixedOutput + OutputSizeUser,
    R: Read,
    H: Write,
    [(); H::OutputSize::USIZE]:,
    GenericArray<u8, H::OutputSize>: From<[u8; H::OutputSize::USIZE]>,
    GenericArray<u8, H::OutputSize>: Into<[u8; H::OutputSize::USIZE]>,
{
    // initial hashing
    let mut hash = GenericArray::<u8, H::OutputSize>::from([0_u8; H::OutputSize::USIZE]);
    let mut hasher = H::new();
    io::copy(&mut reader, &mut hasher)?;
    FixedOutput::finalize_into(hasher, &mut hash);

    // last iterations
    for _ in 1..iter_num {
        let mut hasher = H::new();
        Digest::update(&mut hasher, &*hash);
        FixedOutput::finalize_into(hasher, &mut hash);
    }

    Ok(hash.into())
}

pub fn xof_output_hash<H, R>(mut reader: R, length: usize, iter_num: u64) -> io::Result<Vec<u8>>
where
    H: Digest + ExtendableOutput,
    R: Read,
    H: Write,
{
    let mut hash = vec![0_u8; length];

    // initial hashing
    let mut hasher = H::new();
    io::copy(&mut reader, &mut hasher)?;
    hasher.finalize_xof_into(&mut hash);

    // last iterations
    for _ in 1..iter_num {
        let mut hasher = H::new();
        Digest::update(&mut hasher, &hash);
        hasher.finalize_xof_into(&mut hash);
    }

    Ok(hash)
}
