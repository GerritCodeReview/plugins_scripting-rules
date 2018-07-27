# Move me to <Gerrit's code root>/plugins/external_plugin_deps.bzl

load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "com_eclipsesource_j2v8",
        artifact = "com.eclipsesource.j2v8:j2v8_linux_x86_64:4.8.0",
        sha1 = "dad0e7695388f99ab504fa9f259101394a78eb2f",
    )
