# REST API Static Security Testing

This Jenkins plugin allows you to add automated static application security testing (SAST) tasks automatically performed on API contract files during the CI/CD runs.

API contracts must be in OpenAPI (aka Swagger) format. Both JSON and YAML formats, and both v2 and v3 are supported.

The extension is using 42Crunch Security Audit functionality. 42Crunch Security Audit is a static analysis of the API definition that includes more than 200 checks on best practives and potential vulnerabilities in the way the API defines authentication, authorization, transport, data coming in and going out. See the [API Security Encyclopedia](https://apisecurity.io/encyclopedia/content/api-security-encyclopedia.htm) for details.

You can create a free 42Crunch account at https://platform.42crunch.com/register and then follow the [quick start guide](https://docs.42crunch.com/latest/content/tasks/integrate_jenkins.htm) to configure the extension.
