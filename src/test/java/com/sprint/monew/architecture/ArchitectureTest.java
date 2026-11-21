package com.sprint.monew.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ArchTag("architecture")
@AnalyzeClasses(packages = "com.sprint.monew", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.sprint.monew";

    @ArchTest
    static final ArchRule ADAPTER_IN_DOES_NOT_USE_ADAPTER_OUT =
            noClasses()
                    .that()
                    .resideInAPackage(ROOT + ".adapter.in..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(ROOT + ".adapter.out..")
                    .because("컨트롤러는 UseCase를 거쳐야 해요")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule DOMAIN_DOES_NOT_KNOW_ADAPTER_OR_APPLICATION =
            noClasses()
                    .that()
                    .resideInAPackage(ROOT + ".domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(ROOT + ".adapter..", ROOT + ".application..")
                    .because("도메인은 가장 안쪽 층이라 바깥 층을 몰라야 해요")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule APPLICATION_DOES_NOT_KNOW_ADAPTER =
            noClasses()
                    .that()
                    .resideInAPackage(ROOT + ".application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(ROOT + ".adapter..")
                    .because("애플리케이션은 required 포트로만 바깥을 불러야 해요")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule GLOBAL_DOES_NOT_KNOW_DOMAIN_CODE =
            noClasses()
                    .that()
                    .resideInAPackage(ROOT + ".global..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            ROOT + ".adapter..", ROOT + ".application..", ROOT + ".domain..")
                    .because("global은 여러 도메인이 같이 쓰는 코드라 특정 도메인에 기대면 안 돼요")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule CONTROLLER_USES_ONLY_OWN_PROVIDED =
            classes()
                    .that()
                    .resideInAPackage(ROOT + ".adapter.in.controller..")
                    .should(
                            onlyUseApplicationOf(
                                    Pattern.compile("\\.adapter\\.in\\.controller\\.([a-z]+)"),
                                    true))
                    .because("컨트롤러는 자기 도메인의 UseCase(provided)만 불러야 해요")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule ADAPTER_OUT_USES_ONLY_OWN_APPLICATION =
            classes()
                    .that()
                    .resideInAPackage(ROOT + ".adapter.out..")
                    .should(
                            onlyUseApplicationOf(
                                    Pattern.compile("\\.adapter\\.out\\.([a-z]+)"), false))
                    .because("DB 어댑터는 자기 도메인의 required 포트만 구현해야 해요")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule OTHER_DOMAINS_ONLY_THROUGH_INTERFACES_OR_EVENTS =
            classes()
                    .that()
                    .resideInAPackage(ROOT + ".application..")
                    .should(callOtherDomainsOnlyThroughInterfacesOrEvents())
                    .because("다른 도메인의 구현체나 required 포트에 기대면 그 도메인 내부가 바뀔 때 같이 깨져요")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule NO_CYCLES_BETWEEN_APPLICATION_DOMAINS =
            slices().matching(ROOT + ".application.(*)..")
                    .should()
                    .beFreeOfCycles()
                    .because("서로 부르는 순환이 생기면 한쪽을 고칠 때 다른 쪽까지 같이 깨져요")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule NO_CYCLES_BETWEEN_ENTITY_DOMAINS =
            slices().matching(ROOT + ".domain.(*)..")
                    .should()
                    .beFreeOfCycles()
                    .because("엔티티끼리 순환하면 저장·삭제 순서가 꼬이기 쉬워요")
                    .allowEmptyShould(true);

    private static final Pattern APPLICATION =
            Pattern.compile("^" + Pattern.quote(ROOT) + "\\.application\\.([a-z]+)(\\..*)?$");

    private static Optional<String> applicationDomain(JavaClass target) {
        Matcher m = APPLICATION.matcher(target.getPackageName());
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private static Optional<String> domainOf(JavaClass origin, Pattern pattern) {
        Matcher m = pattern.matcher(origin.getPackageName());
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private static ArchCondition<JavaClass> onlyUseApplicationOf(
            Pattern originPattern, boolean providedOnly) {
        String description =
                providedOnly ? "같은 도메인의 application.provided만 사용" : "같은 도메인의 application만 사용";
        return new ArchCondition<>(description) {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String> own = domainOf(origin, originPattern);
                if (own.isEmpty()) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    Optional<String> targetDomain = applicationDomain(target);
                    if (targetDomain.isEmpty()) {
                        continue;
                    }
                    boolean sameDomain = targetDomain.get().equals(own.get());
                    boolean provided =
                            target.getPackageName()
                                    .contains(".application." + targetDomain.get() + ".provided");
                    if (!sameDomain || (providedOnly && !provided)) {
                        events.add(
                                SimpleConditionEvent.violated(
                                        dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> callOtherDomainsOnlyThroughInterfacesOrEvents() {
        return new ArchCondition<>("다른 도메인은 인터페이스나 이벤트로만 사용") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String> own = applicationDomain(origin);
                if (own.isEmpty()) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    Optional<String> targetDomain = applicationDomain(target);
                    if (targetDomain.isEmpty() || targetDomain.get().equals(own.get())) {
                        continue;
                    }
                    String pkg = target.getPackageName();
                    String base = ROOT + ".application." + targetDomain.get();
                    boolean required = pkg.startsWith(base + ".required");
                    boolean event = pkg.startsWith(base + ".event");
                    boolean providedDto = pkg.startsWith(base + ".provided");
                    if (required || !(target.isInterface() || event || providedDto)) {
                        events.add(
                                SimpleConditionEvent.violated(
                                        dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }
}
