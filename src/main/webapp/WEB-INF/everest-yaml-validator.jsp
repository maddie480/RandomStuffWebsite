<%@ page import="java.util.List, ovh.maddie480.randomstuff.frontend.EverestYamlValidatorService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4, static org.apache.commons.text.StringEscapeUtils.escapeEcmaScript"%>

<h1>everest.yaml validator</h1>

Want to know if your everest.yaml is valid? Send it here, and this service will check:
<ul>
    <li>If the file is a valid YAML file, and it has all required fields</li>
    <li>If all dependencies exist in the dependency downloader database</li>
</ul>

<form method="POST" enctype="multipart/form-data">
    <input type="file" accept=".yaml,.yml" id="file" name="file" required>
    <input type="hidden" name="outputFormat" value="html">
    <input type="submit" class="btn btn-primary" value="Validate!">
</form>

<br/>

<% if (request.getAttribute("parseError") != null) { %>
    <div class="alert alert-danger">
        <b>Your everest.yaml file is not valid.</b> An error occurred while parsing it:
        <pre><%= escapeHtml4((String) request.getAttribute("parseError")) %></pre>
        <p class="error-description">
            This probably means your file is not valid YAML or that it does not match the structure of an everest.yaml file.
            Make sure it looks like this one, use spaces (not tabs) and check text is aligned in the same way:
        </p>

<!-- pre blocks are sensitive to indentation so it has to be over there on the left aaaa -->
<pre>
- Name: YourModName
  Version: 1.0.0
  Dependencies:
    - Name: DependencyName1
      Version: 1.0.0
    - Name: DependencyName2
      Version: 1.0.0
</pre>
        </div>
    <% } else if (request.getAttribute("validationErrors") != null) { %>
        <div class="alert alert-warning">
            <b>There are issues with your everest.yaml file:</b>
            <ul>
                <% for (String issue : (List<String>) request.getAttribute("validationErrors")) { %>
                    <li><%= escapeHtml4(issue) %></li>
                <% } %>
            </ul>
        </div>
    <% } else if (request.getAttribute("modInfo") != null) { %>
        <% List<EverestYamlValidatorService.EverestModuleMetadata> modsInfo = (List<EverestYamlValidatorService.EverestModuleMetadata>) request.getAttribute("modInfo"); %>
        <div class="alert alert-success">
            <b>Your everest.yaml file seems valid!</b>

            <% for (EverestYamlValidatorService.EverestModuleMetadata modInfo : modsInfo) { %>
                <%= modsInfo.size() == 1 ? "Your mod is called" : "It contains a mod called" %>
                <b><%= escapeHtml4(modInfo.Name) %></b> version <%= escapeHtml4(modInfo.Version) %> and depends on:
                <ul>
                    <% for (EverestYamlValidatorService.EverestModuleMetadata dependency : modInfo.Dependencies) { %>
                        <li>
                            <b><%= escapeHtml4(dependency.Name) %></b> version <%= escapeHtml4(dependency.Version) %>
                            <i>(latest version is <%= escapeHtml4(dependency.LatestVersion) %>)</i>
                        </li>
                    <% } %>
                    <% for (EverestYamlValidatorService.EverestModuleMetadata dependency : modInfo.OptionalDependencies) { %>
                        <li>
                            <i>Optional:</i> <b><%= escapeHtml4(dependency.Name) %></b> version <%= escapeHtml4(dependency.Version) %>
                            <i>(latest version is <%= escapeHtml4(dependency.LatestVersion) %>)</i>
                        </li>
                    <% } %>
                    <% if (modInfo.Dependencies.isEmpty() && modInfo.OptionalDependencies.isEmpty()) { %>
                        <li>Nothing!</li>
                    <% } %>
                </ul>
            <% } %>
        </div>

        <% if (request.getAttribute("latestVersionsYaml") != null) { %>
            <button id="download-latest-versions-yaml" class="btn btn-outline-dark">
                &#x1F4E5; Download everest.yaml with updated dependencies
            </button>
        <% } %>
    <% } else { %>
        <% if (request.getAttribute("sampleYaml") != null) { %>
            <div class="alert alert-success">
                <b>Need help getting started?</b> Here is a sample everest.yaml that only has Everest as a dependency, you can open it with any text editor:

                <button id="download-sample-yaml" class="btn btn-outline-dark">
                    &#x1F4E5; Download sample everest.yaml
                </button>

                Replace <code>YourModName</code> with your mod's name (do not use characters forbidden in Windows file names),
                and add your dependencies, making sure the <code>Name</code>s and <code>Version</code>s are all lined up vertically.
                The L&#x00f6;nn map editor can also add missing dependencies to your mod for you, in the Map > Dependencies menu.
            </div>

            <script nonce="<%= request.getAttribute("nonce") %>">
                document.getElementById("download-sample-yaml").addEventListener("click", function() {
                    const yamlContents = "<%= escapeEcmaScript((String) request.getAttribute("sampleYaml")) %>";
                    download(yamlContents, "everest.yaml", "text/yaml");
                });
            </script>
        <% } %>

        <div class="alert alert-info">
            <b>This page only validates the everest.yaml files you place at the root of your mod.</b>
            If you want to validate the syntax of other YAML files (like map meta.yamls), use online tools like
            <a href="https://jsonformatter.org/yaml-validator/" target="_blank" rel="noopener">this one</a>.
        </div>
    <% } %>

    <% if (request.getAttribute("latestVersionsYaml") != null) { %>
        <script nonce="<%= request.getAttribute("nonce") %>">
            document.getElementById("download-latest-versions-yaml").addEventListener("click", function() {
                const yamlContents = "<%= escapeEcmaScript((String) request.getAttribute("latestVersionsYaml")) %>";
                download(yamlContents, "everest.yaml", "text/yaml");
            });
        </script>
    <% } %>
