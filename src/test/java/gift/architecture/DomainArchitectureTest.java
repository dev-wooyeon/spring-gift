package gift.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
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
    void presentationDoesNotDependOnInfrastructure() {
        noClasses()
            .that().resideInAPackage("gift..presentation..")
            .should().dependOnClassesThat().resideInAPackage("gift..infrastructure..")
            .check(productionClasses);
    }

    @Test
    void applicationDoesNotDependOnPresentation() {
        noClasses()
            .that().resideInAPackage("gift..application..")
            .should().dependOnClassesThat().resideInAPackage("gift..presentation..")
            .check(productionClasses);
    }

    @Test
    void domainsDoNotDependOnOtherDomainsInfrastructure() {
        for (String domain : DOMAINS) {
            noClasses()
                .that().resideInAPackage("gift." + domain + "..")
                .should().dependOnClassesThat().resideInAnyPackage(otherDomainsInfrastructure(domain))
                .check(productionClasses);
        }
    }

    private String[] otherDomainsInfrastructure(String domain) {
        return Arrays.stream(DOMAINS)
            .filter(candidate -> !candidate.equals(domain))
            .map(candidate -> "gift." + candidate + ".infrastructure..")
            .toArray(String[]::new);
    }
}
