use crate::jni_helper::{jni_log, GetString};
use crate::transfer::errors::*;
use bczhc_lib::time::get_current_time_millis;
use bczhc_lib::{rw_read, rw_write};
use byteorder::{BigEndian, ReadBytesExt};
use jni::objects::{GlobalRef, JString, JValue};
use jni::sys::{jint, jlong, jobject};
use jni::{AttachGuard, JNIEnv, JavaVM};
use num_traits::FromPrimitive;
use once_cell::sync::Lazy;
use std::fs::{create_dir_all, File};
use std::io;
use std::io::{BufWriter, Cursor, Read, Write};
use std::net::{SocketAddrV4, TcpListener, TcpStream};
use std::path::PathBuf;
use std::sync::RwLock;
use std::thread::spawn;
use tar::Archive;

static JVM: Lazy<RwLock<Option<JavaVM>>> = Lazy::new(|| RwLock::new(None));
static RECEIVE_DONE_CALLBACK: Lazy<RwLock<Option<GlobalRef>>> = Lazy::new(|| RwLock::new(None));
static SAVING_PATH: Lazy<RwLock<Option<String>>> = Lazy::new(|| RwLock::new(None));

pub fn async_start_server(
    env: JNIEnv,
    port: jint,
    saving_path: JString,
    callback: jobject,
) -> Result<jlong> {
    let port = u16::try_from(port).map_err(|_| Error::IllegalArgument)?;
    let b = Box::new(TcpListener::bind(SocketAddrV4::new(
        "0.0.0.0".parse().unwrap(),
        port,
    ))?);

    let listener_clone = b.as_ref().try_clone()?;

    let jvm = env.get_java_vm()?;
    rw_write!(JVM).replace(jvm);

    let global_ref = env.new_global_ref(callback)?;
    rw_write!(RECEIVE_DONE_CALLBACK).replace(global_ref);

    let saving_path = env.get_string_owned(saving_path)?;
    rw_write!(SAVING_PATH).replace(saving_path);

    spawn(move || {
        accept_loop(listener_clone).unwrap();
    });

    Ok(Box::into_raw(b) as usize as jlong)
}

fn accept_loop(listener: TcpListener) -> Result<()> {
    loop {
        let accept = listener.accept().unwrap();
        let stream = accept.0;
        spawn(move || {
            let jvm_guard = rw_read!(JVM);
            let jvm = jvm_guard.as_ref().unwrap();
            let env_guard = jvm.attach_current_thread().unwrap();

            jni_log(*env_guard, &format!("{}", accept.1)).unwrap();

            let receiving_result = handle_connection(stream);
            jni_log(*env_guard, &format!("{:?}", receiving_result)).unwrap();
            let receiving_result = receiving_result.unwrap();

            let callback = rw_read!(RECEIVE_DONE_CALLBACK);
            let callback = callback.as_ref().unwrap();

            let location_jstring = env_guard.new_string(&receiving_result.location).unwrap();

            jni_log(*env_guard, "call").unwrap();
            env_guard
                .call_method(
                    callback,
                    "onReceiveDone",
                    "(IJJLjava/lang/String;)V",
                    &[
                        JValue::Int(receiving_result.mark as jint),
                        JValue::Long(receiving_result.time as jlong),
                        JValue::Long(receiving_result.size as jlong),
                        JValue::Object(location_jstring.into()),
                    ],
                )
                .unwrap();
        });
    }
}

fn handle_connection(mut stream: TcpStream) -> Result<ReceivingResult> {
    let guard = rw_read!(JVM);
    let env_guard = guard.as_ref().unwrap().attach_current_thread()?;

    let mut header_bytes = [0_u8; 8];
    stream.read_exact(&mut header_bytes)?;
    if &header_bytes != HEADER {
        return Err(Error::InvalidHeader);
    }

    let mark = stream.read_u8()?;
    let result = FromPrimitive::from_u8(mark);
    if result.is_none() {
        return Err(Error::InvalidMark);
    }
    let mark: Mark = result.unwrap();

    let timestamp = get_current_time_millis();
    let file_location = new_file_location(timestamp)?;

    let (size, file_location) = match mark {
        Mark::File => {
            let file_name_len = stream.read_u32::<BigEndian>()? as usize;
            let mut file_name_bytes = vec![0_u8; file_name_len];
            stream.read_exact(&mut file_name_bytes)?;

            let file_name = std::str::from_utf8(&file_name_bytes)?;

            jni_log(*env_guard, &format!("file name: {}", file_name))?;

            let mut file_path = PathBuf::from(&file_location);
            create_dir_all(&file_path)?;
            jni_log(*env_guard, &format!("file path: {:?}", file_path))?;
            jni_log(*env_guard, "3")?;
            file_path.push(file_name);

            jni_log(*env_guard, "2")?;
            let file = File::create(&file_path)?;
            let mut writer = BufWriter::new(file);

            jni_log(*env_guard, "4")?;
            let mut size = 0_u64;
            let mut buf = [0_u8; 1024];
            jni_log(*env_guard, "1")?;
            loop {
                let n = stream.read(&mut buf)?;
                if n == 0 {
                    jni_log(*env_guard, "eof")?;
                    break;
                }
                writer.write_all(&buf[..n])?;
                jni_log(*env_guard, &format!("write: {:?}", &buf[..n]))?;
                size += n as u64;
            }

            // let size = io::copy(&mut stream, &mut writer)?;

            jni_log(*env_guard, "copy done")?;

            (
                size,
                String::from(
                    file_path
                        .to_str()
                        .map_or_else(|| Err(Error::InvalidPathName), Ok)?,
                ),
            )
        }
        Mark::Text => {
            let mut cursor = Cursor::new(Vec::new());
            let size = io::copy(&mut stream, &mut cursor)?;
            File::create(&file_location)?.write_all(cursor.get_ref())?;
            (size, file_location)
        }
        Mark::Tar => {
            let mut reader_counter = ReaderCounter::new(&mut stream);
            receive_tar(&mut reader_counter, &file_location)?;
            let size = reader_counter.read_size();
            (size, file_location)
        }
    };

    Ok(ReceivingResult {
        mark,
        time: timestamp,
        location: file_location,
        size,
    })
}

fn receive_tar<R>(reader: &mut R, unpack_dir: &str) -> Result<()>
where
    R: Read,
{
    let jvm_guard = rw_read!(JVM);
    let env_guard = jvm_guard.as_ref().unwrap().attach_current_thread()?;

    let mut archive = Archive::new(reader);

    // for x in archive.entries().unwrap() {
    //     jni_log(*env_guard, &format!("{:?}", x.unwrap().path()))?;
    // }
    //
    // jni_log(*env_guard, unpack_dir)?;

    archive.unpack(&unpack_dir)?;

    Ok(())
}

fn new_file_location(timestamp: u64) -> Result<String> {
    let guard = rw_read!(SAVING_PATH);
    let saving_path = guard.as_ref().unwrap();
    let mut path = PathBuf::from(saving_path);

    if !path.exists() {
        create_dir_all(&path)?;
    }

    path.push(timestamp.to_string());
    let path_str = path
        .to_str()
        .map_or_else(|| Err(Error::InvalidPathName), Ok)?;
    Ok(String::from(path_str))
}

#[derive(Debug)]
struct ReceivingResult {
    mark: Mark,
    time: u64,
    location: String,
    size: u64,
}

const HEADER: &[u8; 8] = b"bczhc\0\0\0";

#[derive(num_derive::FromPrimitive, Copy, Clone, Debug)]
pub enum Mark {
    File = 1,
    Text = 2,
    Tar = 3,
}

struct ReaderCounter<'a, R>
where
    R: Read,
{
    size: u64,
    reader: &'a mut R,
}

impl<'a, R> ReaderCounter<'a, R>
where
    R: Read,
{
    fn new(reader: &'a mut R) -> Self {
        Self { size: 0, reader }
    }

    fn read_size(&self) -> u64 {
        self.size
    }
}

impl<'a, W> Read for ReaderCounter<'a, W>
where
    W: Read,
{
    fn read(&mut self, buf: &mut [u8]) -> io::Result<usize> {
        let read_size = self.reader.read(buf)?;
        self.size += read_size as u64;
        Ok(read_size)
    }
}
