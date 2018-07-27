package(
    default_visibility = ["//visibility:public"],
)

load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_DEPS_NEVERLINK")
load("//plugins/scripting-rules:plugin.bzl", "SELF_PREFIX", "ENGINES_TO_ENABLE")

java_library(
    name = "lua",
    srcs = glob(["**/*.java"]),
    deps = PLUGIN_DEPS_NEVERLINK + [
        "@org_luaj_luaj_jse//jar",
        SELF_PREFIX + "/engines",
        SELF_PREFIX + "/utils",
    ],
)
