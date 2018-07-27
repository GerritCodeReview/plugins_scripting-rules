package(
    default_visibility = ["//visibility:public"],
)

load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_DEPS_NEVERLINK")
load("//plugins/scripting-rules:plugin.bzl", "SELF_PREFIX", "ENGINES_TO_ENABLE")

java_library(
    name = "js",
    srcs = glob(["**/*.java"]),
    deps = PLUGIN_DEPS_NEVERLINK + [
        "@com_eclipsesource_j2v8//jar",
        SELF_PREFIX + "/engines",
        SELF_PREFIX + "/utils",
    ],
)
