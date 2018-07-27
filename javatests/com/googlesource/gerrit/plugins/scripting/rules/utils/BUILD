load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_TEST_DEPS")
load("//plugins/scripting-rules:plugin.bzl", "SELF_PREFIX")
load("//tools/bzl:junit.bzl", "junit_tests")

junit_tests(
    name = "utils",
    srcs = glob(["**/*.java"]),
    deps = PLUGIN_TEST_DEPS + [
        "//plugins/scripting-rules",
        SELF_PREFIX + "/utils",
    ],
)
