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
    static final ArchRule identity_is_foundational =
            noClasses()
                    .that()
                    .resideInAPackage("..identity..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..enterprise..",
                            "..assessment..",
                            "..simulation..",
                            "..guidance..",
                            "..catalog..",
                            "..system..");

    @ArchTest
    static final ArchRule enterprise_does_not_depend_on_evidence_owners =
            noClasses()
                    .that()
                    .resideInAPackage("..enterprise..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..assessment..",
                            "..simulation..",
                            "..guidance..",
                            "..catalog..",
                            "..system..");

    @ArchTest
    static final ArchRule assessment_does_not_depend_on_simulation_or_guidance =
            noClasses()
                    .that()
                    .resideInAPackage("..assessment..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..simulation..",
                            "..guidance..",
                            "..enterprise..",
                            "..catalog..",
                            "..system..");

    @ArchTest
    static final ArchRule simulation_does_not_depend_on_consumers =
            noClasses()
                    .that()
                    .resideInAPackage("..simulation..")
                    // Approved legacy v1 adapters retain their original catalog dependency.
                    .and()
                    .doNotHaveSimpleName("SimulationAttemptServiceImpl")
                    .and()
                    .doNotHaveSimpleName("SimulationQueryServiceImpl")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..guidance..", "..catalog..", "..system..");

    @ArchTest
    static final ArchRule assessment_persistence_is_private =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("..assessment..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..assessment.dao..",
                            "..assessment.entity..",
                            "..assessment.service.impl..");

    @ArchTest
    static final ArchRule simulation_persistence_is_private =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("..simulation..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..simulation.dao..",
                            "..simulation.entity..",
                            "..simulation.service.impl..");

    @ArchTest
    static final ArchRule enterprise_persistence_is_private =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("..enterprise..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..enterprise.dao..", "..enterprise.service.impl..");

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
