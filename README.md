# REST API Static Security Testing

The REST API Static Security Testing plugin lets you add an automatic static application security testing (SAST) task to your CI/CD pipelines. The task checks your OpenAPI files for their quality and security from a simple Git push to your project repository when the CI/CD pipeline runs.

The plugin is powered by 42Crunch [API Security Audit](https://docs.42crunch.com/latest/content/concepts/api_contract_security_audit.htm). Security Audit performs a static analysis of the API definition that includes more than 200 checks on best practices and potential vulnerabilities on how the API defines authentication, authorization, transport, and data coming in and going out. For more details on the checks, see [API Security Encyclopedia](https://apisecurity.io/encyclopedia/content/api-security-encyclopedia.htm).

As a result of the security testing, your APIs get an audit score, with 100 points meaning the most secure, best defined API. By default, the threshold score for the build task to pass is 75 points for each audited API, but you can change the minimum score in the settings of the pipeline task.

API contracts must follow the OpenAPI Specification (OAS) (formely Swagger). Both OAS v2 and v3, and both JSON and YAML formats are supported.

You can create a free 42Crunch account at https://platform.42crunch.com/register, and then configure the plugin.

## Quick start

1. Install the plugin.
2. Add the build step to the job.
3. Add new `42Crunch API Token` credential.
4. Create an API token in 42Crunch platform and copy its value into the credential.
5. Save the job configuration and run the job.
6. Click the links in the task output for detailed reports.

For more details, see the [full documentation](https://docs.42crunch.com/latest/content/tasks/integrate_jenkins.htm).

## Discover APIs

By default, the task locates all OpenAPI files in your project and submits them for static security testing. You can include or exclude specific paths from the discovery phase can omit the discovery phase completely by adding a task configuration file `42c-conf.yaml` in the root of your repository and specifying rules for the discovery phase. For more details, see the [documentation](https://docs.42crunch.com/latest/content/tasks/integrate_jenkins.htm).

All discovered APIs are uploaded to an API collection in 42Crunch Platform. This collection is created at the
first run of the task, and is tied to the repository name and branch name it was created from.
During the subsequent task runs, the APIs in the collection are kept in sync with the changes in your repository.

The task uses the parameters `repositoryName` and `branchName` to access infromation about respository name and branch name.
Depending on your Jenkins configuration these must be changed to allow task to access repository name and branch name.

By default `repositoryName` uses `${GIT_URL}` environment variable and `branchName` uses `${GIT_LOCAL_BRANCH}`.
If you are using "Multibranch Pipeline" and Jenkinsfile you have to explicitly pass these parameters setting `repositoryName` to `${env.GIT_URL}` and `branchName` to `${env.BRANCH_NAME}`.

## Fine-tune the build task

You can add a task configuration file `42c-conf.yaml` in the root of your repository, and to fine-tune the success/failure criteria. For example, you can choose on whether to accept invalid API contracts, or define a cut-off on a certain level of issue severity.

For more details, see the [documentation](https://docs.42crunch.com/latest/content/tasks/integrate_jenkins.htm).

## Support

The plugin is maintained by support@42crunch.com. If you run into an issue, or have a question not answered here, you can create a support ticket at [support.42crunch.com](https://support.42crunch.com/).

If you’re reporting an issue, please include:

- the version of the plugin
- relevant logs and error messages
- steps to reproduce
