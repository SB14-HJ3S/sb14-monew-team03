package com.sprint.monew.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

// architecture 태그를 붙이지 않아 일반 test와 build에서도 위반 시 실패한다.
@AnalyzeClasses(packages = "com.sprint.monew", importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationServiceArchitectureTest {

    private static final String APPLICATION = "com.sprint.monew.application..";
    private static final String SERVICES = "com.sprint.monew.application.*.provided.service..";

    @ArchTest
    static final ArchRule SERVICE_INTERFACES_RESIDE_IN_PROVIDED_SERVICE =
            classes()
                    .that()
                    .resideInAPackage(APPLICATION)
                    .and()
                    .areInterfaces()
                    .and()
                    .haveSimpleNameEndingWith("Service")
                    .should()
                    .resideInAPackage(SERVICES)
                    .because("내부 Service 인터페이스는 provided.service에 모아야 해요");

    @ArchTest
    static final ArchRule SERVICES_ARE_ONLY_USED_BY_APPLICATION =
            classes()
                    .that()
                    .resideInAPackage(SERVICES)
                    .and()
                    .areInterfaces()
                    .should()
                    .onlyHaveDependentClassesThat()
                    .resideInAPackage(APPLICATION)
                    .because("Service는 application 내부에서만 사용하고 외부에서는 UseCase를 불러야 해요");
}
