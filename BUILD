load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS")
load("//plugins/scripting-rules:plugin.bzl", "SELF_PREFIX")

gerrit_plugin(
    name = "scripting-rules",
    srcs = [
        "java/com/googlesource/gerrit/plugins/scripting/rules/Module.java",
    ],
    manifest_entries = [
        "Gerrit-PluginName: scripted-rules",
        "Gerrit-Module: com.googlesource.gerrit.plugins.scripting.rules.Module",
        "Gerrit-BatchModule: com.googlesource.gerrit.plugins.scripting.rules.Module",
    ],
    resources = glob(["resources/**/*"]),
    deps = [
        SELF_PREFIX + "/engines:module",
    ],
)
