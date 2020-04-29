package nl.praegus.fitnesse.symbols.MavenProjectVersions;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ProjectDependencyInfo {
    private List<DependencyInfo> dependenciesInfo = new ArrayList<>();
    private final static Set<String> IGNORE_DEPENDENCIES = new HashSet<>(Arrays.asList(
            "jaxb-core",
            "jaxb-impl",
            "jaxb-api",
            "activation",
            "junit"
    ));

    public ProjectDependencyInfo() throws FileNotFoundException {
        // Add plugin information
        dependenciesInfo.add(new DependencyInfo("nl.praegus", "toolchain-fitnesse-plugin"));

        // Add Dependency information
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(System.getProperty("user.dir") + "/../pom.xml"));
            List<Dependency> dependencies = model.getDependencies();
            for (Dependency dep : dependencies) {
                String artifact = dep.getArtifactId();
                if (!IGNORE_DEPENDENCIES.contains(artifact)) {
                    dependenciesInfo.add(getDependencyVersionInformation(dep, model, artifact));
                }
            }
        } catch (IOException | XmlPullParserException ignored) {
            throw new FileNotFoundException("POM not found!");
        }
    }

    public List<DependencyInfo> getDependencyInfo() {
        return dependenciesInfo;
    }

    private DependencyInfo getDependencyVersionInformation(Dependency dependency, Model model, String artifact) {
        String group = dependency.getGroupId();

        String version = dependency.getVersion();
        return new DependencyInfo(group, artifact, version, model);
    }

}
