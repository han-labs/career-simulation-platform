package edu.hcmute.careersim.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "edu.hcmute.careersim",
        importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule controllers_must_not_access_internal_persistence_or_implementation =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..dao..", "..entity..", "..service.impl..");

    @ArchTest
    static final ArchRule data_access_must_not_depend_on_controller_or_service =
            noClasses()
                    .that()
                    .resideInAPackage("..dao..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..controller..", "..service..");

    @ArchTest
    static final ArchRule entities_must_not_depend_on_web_or_security =
            noClasses()
                    .that()
                    .resideInAPackage("..entity..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.web..",
                            "org.springframework.security..",
                            "..controller..",
                            "..dto..");
}
