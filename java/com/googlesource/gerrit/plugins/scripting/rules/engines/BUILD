package(
    default_visibility = ["//visibility:public"],
)

load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_DEPS_NEVERLINK")
load("//plugins/scripting-rules:plugin.bzl", "SELF_PREFIX", "ENGINES_TO_ENABLE")

java_library(
    name = "engines",
    srcs = glob(["RuleEngine.java"]),
    deps = PLUGIN_DEPS_NEVERLINK + [
        SELF_PREFIX + "/utils",
    ],
)

ENGINES_LABELS = [
    SELF_PREFIX + "/engines/" + name
    for name in ENGINES_TO_ENABLE
]

java_library(
    name = "module",
    srcs = ["EnginesModule.java"],
    deps = PLUGIN_DEPS_NEVERLINK + [
        ":engines",
        SELF_PREFIX + "/rule",
    ] + ENGINES_LABELS,
)
