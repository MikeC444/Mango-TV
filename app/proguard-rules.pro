# Mango TV keeps its own model layer serialisable and reflection-free, so the
# default optimised ruleset is sufficient. Rules for kotlinx.serialization,
# Room and Media3 are contributed by those libraries' consumer rules.

# Preserve line numbers for readable crash reports, but hide the source file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
