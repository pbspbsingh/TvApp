use std::thread;

use android_logger::Config;
use jni::objects::{JClass, JString};
use jni::JNIEnv;
use log::*;
use tv_shows_server::start_server;

#[no_mangle]
pub extern "system" fn Java_com_pbs_server_http_ServerUtil_startServer(
    mut env: JNIEnv,
    _class: JClass,
    folder: JString,
) {
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Info)
            .with_tag("RustServer"),
    );

    let cache_folder = env
        .get_string(&folder)
        .map(String::from)
        .expect("Couldn't get String from 'JString'");
    info!("Starting backend server with cache folder: {cache_folder}");

    thread::spawn(move || {
        start_server(&cache_folder, 2, 2, 3000)
            .unwrap_or_else(|e| panic!("Error while starting rust server: {e:?}"));
    });
}
