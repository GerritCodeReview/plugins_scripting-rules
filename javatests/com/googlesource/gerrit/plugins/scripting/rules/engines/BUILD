load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS")
load("//plugins/scripting-rules:plugin.bzl", "SELF_PREFIX")
load("//tools/bzl:junit.bzl", "junit_tests")

junit_tests(
    name = "engines",
    srcs = glob(["**/*.java"]),
    visibility = ["//visibility:public"],
    deps = PLUGIN_TEST_DEPS + ["//plugins/scripting-rules"],
)
