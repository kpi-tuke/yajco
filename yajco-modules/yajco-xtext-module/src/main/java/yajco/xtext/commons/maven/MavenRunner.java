package yajco.xtext.commons.maven;

import org.apache.maven.shared.invoker.*;
import yajco.xtext.commons.settings.XtextProjectSettings;

import java.io.File;
import java.util.Collections;

public class MavenRunner {

    public static void executeMavenCompile() throws MavenInvocationException {
        System.out.println("Running `mvn compile` ...");
        XtextProjectSettings settings = XtextProjectSettings.getInstance();

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(settings.getParentProjectPomPath()));
        request.setGoals(Collections.singletonList("compile"));

        Invoker invoker = new DefaultInvoker();
        invoker.execute(request);
    }

}