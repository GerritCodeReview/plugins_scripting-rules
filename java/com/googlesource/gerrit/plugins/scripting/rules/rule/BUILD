package(
    default_visibility = ["//visibility:public"],
)

load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS_NEVERLINK")
load("//plugins/scripting-rules:plugin.bzl", "SELF_PREFIX", "ENGINES_TO_ENABLE")

java_library(
    name = "rule",
    srcs = ["ScriptedRule.java"],
    deps = PLUGIN_DEPS_NEVERLINK + [
        SELF_PREFIX + "/utils",
        SELF_PREFIX + "/engines",
    ],
)
