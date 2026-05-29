package gift.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainArchitectureTest {
    private static final String[] DOMAINS = {
        "auth",
        "catalog",
        "member",
        "notification",
        "order",
        "wish"
    };

    private final JavaClasses productionClasses = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages("gift");

    @Test
    @DisplayName("presentation 레이어는 infrastructure 레이어에 직접 의존하지 않는다")
    void presentationDoesNotDependOnInfrastructure() {
        // when & then
        noClasses()
            .that().resideInAPackage("gift..presentation..")
            .should().dependOnClassesThat().resideInAPackage("gift..infrastructure..")
            .check(productionClasses);
    }

    @Test
    @DisplayName("application 레이어는 presentation 레이어에 의존하지 않는다")
    void applicationDoesNotDependOnPresentation() {
        // when & then
        noClasses()
            .that().resideInAPackage("gift..application..")
            .should().dependOnClassesThat().resideInAPackage("gift..presentation..")
            .check(productionClasses);
    }

    @Test
    @DisplayName("도메인은 다른 도메인의 infrastructure 레이어에 직접 의존하지 않는다")
    void domainsDoNotDependOnOtherDomainsInfrastructure() {
        // when & then
        for (String domain : DOMAINS) {
            noClasses()
                .that().resideInAPackage("gift." + domain + "..")
                .should().dependOnClassesThat().resideInAnyPackage(otherDomainsInfrastructure(domain))
                .check(productionClasses);
        }
    }

    @Test
    @DisplayName("order application/domain 레이어는 다른 도메인 모델에 직접 의존하지 않는다")
    void orderApplicationAndDomainDoNotDependOnOtherDomainModels() {
        // when & then
        noClasses()
            .that().resideInAnyPackage("gift.order.application..", "gift.order.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "gift.catalog.domain..",
                "gift.member.domain..",
                "gift.wish.domain.."
            )
            .check(productionClasses);
    }

    @Test
    @DisplayName("wish application/domain 레이어는 다른 도메인 모델에 직접 의존하지 않는다")
    void wishApplicationAndDomainDoNotDependOnOtherDomainModels() {
        // when & then
        noClasses()
            .that().resideInAnyPackage("gift.wish.application..", "gift.wish.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "gift.catalog.domain..",
                "gift.member.domain..",
                "gift.order.domain.."
            )
            .check(productionClasses);
    }

    @Test
    @DisplayName("도메인 간 어댑터는 infrastructure 레이어에만 둔다")
    void crossDomainAdaptersStayInInfrastructure() {
        // when & then
        noClasses()
            .that().resideOutsideOfPackage("gift..infrastructure..")
            .should().haveSimpleNameEndingWith("Adapter")
            .check(productionClasses);
    }

    private String[] otherDomainsInfrastructure(String domain) {
        return Arrays.stream(DOMAINS)
            .filter(candidate -> !candidate.equals(domain))
            .map(candidate -> "gift." + candidate + ".infrastructure..")
            .toArray(String[]::new);
    }
}
