package com.lutzseverino.streamguard;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.lutzseverino.streamguard",
        importOptions = ImportOption.DoNotIncludeTests.class
)
final class ProjectStructureTest {

    @ArchTest
    static final ArchRule DOMAIN_IS_PURE = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..application..",
                    "..bootstrap..",
                    "..config..",
                    "..i18n..",
                    "..infrastructure..",
                    "..platform..",
                    "org.bukkit..",
                    "com.google.gson..",
                    "net.kyori.."
            );

    @ArchTest
    static final ArchRule APPLICATION_IGNORES_ADAPTERS = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..bootstrap..",
                    "..config..",
                    "..i18n..",
                    "..infrastructure..",
                    "..platform..",
                    "org.bukkit..",
                    "com.google.gson..",
                    "net.kyori.."
            );
}
